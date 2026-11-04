package internship_registration.service;

import internship_registration.dto.EmailVerificationTokenResponse;
import internship_registration.entity.UserVerificationOtp;
import internship_registration.entity.enums.OtpPurpose;
import internship_registration.exception.DuplicateResourceException;
import internship_registration.exception.OtpException;
import internship_registration.repository.UserRepository;
import internship_registration.repository.UserVerificationOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Email verification BEFORE the account is created (student registration).
 * Uses the user_verification_otp table with user_id = NULL and email set.
 *
 *   POST /api/auth/register/email/send-otp    -> emails a 6-digit code
 *   POST /api/auth/register/email/verify-otp  -> returns emailVerificationToken
 *   POST /api/auth/register/student           -> must carry that token; consumeToken() is called there
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationEmailVerificationService {

    private static final OtpPurpose PURPOSE = OtpPurpose.EMAIL_VERIFICATION;

    private final UserVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.otp.max-per-hour:5}")
    private int maxPerHour;

    // How long the student has to finish the form after verifying the email
    @Value("${app.registration.email-token-minutes:30}")
    private int tokenMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    // ------------------------------------------------------------------ SEND
    @Transactional
    public void sendOtp(String rawEmail) {
        String email = normalize(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("This email is already registered. Please log in instead.");
        }

        LocalDateTime now = LocalDateTime.now();

        Optional<UserVerificationOtp> last =
                otpRepository.findFirstByEmailAndUserIdIsNullAndPurposeOrderByCreatedAtDescIdDesc(email, PURPOSE);
        if (last.isPresent() && last.get().getCreatedAt() != null
                && last.get().getCreatedAt().isAfter(now.minusSeconds(resendCooldownSeconds))) {
            long wait = resendCooldownSeconds - Duration.between(last.get().getCreatedAt(), now).getSeconds();
            throw new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + Math.max(wait, 1) + " seconds before requesting a new code.");
        }
        if (otpRepository.countByEmailAndUserIdIsNullAndPurposeAndCreatedAtAfter(email, PURPOSE, now.minusHours(1))
                >= maxPerHour) {
            throw new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many codes requested for this email. Please try again after an hour.");
        }

        // Only the newest code (and the token it produces) is ever valid
        otpRepository.invalidateActiveByEmail(email, PURPOSE, now);
        otpRepository.retireTokensByEmail(email, PURPOSE, now);

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        otpRepository.save(UserVerificationOtp.builder()
                .userId(null)
                .email(email)
                .purpose(PURPOSE)
                .otpHash(passwordEncoder.encode(otp))
                .attempts(0)
                .expiresAt(now.plusMinutes(expiryMinutes))
                .build());

        // Email only after the code is saved
        final int expiry = expiryMinutes;
        runAfterCommit(() -> emailService.sendOtpEmail(email, null, otp, PURPOSE, expiry));
    }

    // ------------------------------------------------------------------ VERIFY
    // noRollbackFor: the "attempts + 1" write must be saved even though we throw
    @Transactional(noRollbackFor = OtpException.class)
    public EmailVerificationTokenResponse verifyOtp(String rawEmail, String otp) {
        String email = normalize(rawEmail);
        UserVerificationOtp record = otpRepository
                .findFirstByEmailAndUserIdIsNullAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(email, PURPOSE)
                .orElseThrow(OtpException::invalid);

        LocalDateTime now = LocalDateTime.now();

        if (record.getExpiresAt().isBefore(now)) {
            record.setUsedAt(now);
            throw OtpException.invalid();
        }
        if (record.getAttempts() >= maxAttempts) {
            record.setUsedAt(now);
            throw OtpException.tooManyAttempts();
        }
        if (otp == null || !passwordEncoder.matches(otp.trim(), record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            if (record.getAttempts() >= maxAttempts) {
                record.setUsedAt(now); // burn the code after the last allowed miss
                throw OtpException.tooManyAttempts();
            }
            throw OtpException.invalid();
        }

        // Correct code: it is used up, and a one-time token is issued for the registration form
        String token = newToken();
        record.setUsedAt(now);
        record.setVerificationTokenHash(sha256(token));
        record.setTokenExpiresAt(now.plusMinutes(tokenMinutes));
        otpRepository.save(record);

        return EmailVerificationTokenResponse.builder()
                .message("Email verified. Please complete the registration within " + tokenMinutes + " minutes.")
                .email(email)
                .emailVerificationToken(token)
                .expiresInMinutes(tokenMinutes)
                .build();
    }

    // ------------------------------------------------------------------ CONSUME (called by AuthService)
    /**
     * Checks the token belongs to this email, is not used and not expired, then marks it used.
     * Runs inside the registration transaction: if the registration fails, the token stays usable.
     */
    @Transactional
    public void consumeToken(String token, String rawEmail) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Please verify your email with the OTP before registering");
        }
        String email = normalize(rawEmail);
        UserVerificationOtp record = otpRepository.findByVerificationTokenHash(sha256(token.trim()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Email verification is invalid. Please verify your email again."));

        LocalDateTime now = LocalDateTime.now();
        if (record.getUserId() != null || record.getEmail() == null || !record.getEmail().equals(email)) {
            throw new IllegalArgumentException("The verified email does not match the email in the form");
        }
        if (record.getTokenUsedAt() != null) {
            throw new IllegalArgumentException(
                    "This email verification was already used. Please verify your email again.");
        }
        if (record.getTokenExpiresAt() == null || record.getTokenExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Email verification has expired. Please verify your email again.");
        }
        record.setTokenUsedAt(now);
        otpRepository.save(record);
    }

    // ------------------------------------------------------------------ HELPERS
    public static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}