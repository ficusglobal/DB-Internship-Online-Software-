package internship_registration.service;

import internship_registration.dto.ResetPasswordRequest;
import internship_registration.entity.User;
import internship_registration.entity.UserVerificationOtp;
import internship_registration.entity.enums.OtpPurpose;
import internship_registration.exception.OtpException;
import internship_registration.repository.UserRepository;
import internship_registration.repository.UserVerificationOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final UserRepository userRepository;
    private final UserVerificationOtpRepository otpRepository;
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

    private final SecureRandom secureRandom = new SecureRandom();

    // ------------------------------------------------------------------ EMAIL VERIFICATION

    /** Always returns normally, so the caller can't tell whether the email exists. */
    @Transactional
    public void sendEmailVerificationOtp(String email) {
        findUser(email)
                .filter(u -> u.isActive() && !u.isEmailVerified())
                .ifPresent(u -> issueOtp(u, OtpPurpose.EMAIL_VERIFICATION));
    }

    // noRollbackFor: the "attempts + 1" write must be committed even though we throw
    @Transactional(noRollbackFor = OtpException.class)
    public void verifyEmail(String email, String otp) {
        User user = findUser(email).orElseThrow(OtpException::invalid);
        consumeOtp(user, OtpPurpose.EMAIL_VERIFICATION, otp);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    // ------------------------------------------------------------------ PASSWORD RESET

    @Transactional
    public void sendPasswordResetOtp(String email) {
        findUser(email)
                .filter(User::isActive)
                .ifPresent(u -> issueOtp(u, OtpPurpose.PASSWORD_RESET));
    }

    @Transactional(noRollbackFor = OtpException.class)
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User user = findUser(request.getEmail()).orElseThrow(OtpException::invalid);
        consumeOtp(user, OtpPurpose.PASSWORD_RESET, request.getOtp());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Receiving the code at this address proves ownership of the mailbox
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    // ------------------------------------------------------------------ INTERNALS

    private Optional<User> findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    private void issueOtp(User user, OtpPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Resend cooldown (silently skipped so responses stay uniform)
        Optional<UserVerificationOtp> last = otpRepository
                .findFirstByUserIdAndPurposeOrderByCreatedAtDescIdDesc(user.getId(), purpose);
        if (last.isPresent() && last.get().getCreatedAt().isAfter(now.minusSeconds(resendCooldownSeconds))) {
            log.info("{} OTP request for user {} skipped: resend cooldown active", purpose, user.getId());
            return;
        }

        // 2. Hourly cap to stop email flooding
        long sentLastHour = otpRepository
                .countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, now.minusHours(1));
        if (sentLastHour >= maxPerHour) {
            log.warn("{} OTP request for user {} skipped: hourly limit reached", purpose, user.getId());
            return;
        }

        // 3. Retire older codes, then create a new one
        otpRepository.invalidateActive(user.getId(), purpose, now);

        String otp = generateOtp();
        otpRepository.save(UserVerificationOtp.builder()
                .userId(user.getId())
                .purpose(purpose)
                .otpHash(passwordEncoder.encode(otp))
                .attempts(0)
                .expiresAt(now.plusMinutes(expiryMinutes))
                .build());

        sendAfterCommit(user.getEmail(), user.getName(), otp, purpose);
    }

    private void consumeOtp(User user, OtpPurpose purpose, String otp) {
        UserVerificationOtp record = otpRepository
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId(), purpose)
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

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            if (record.getAttempts() >= maxAttempts) {
                record.setUsedAt(now); // burn the code after the last allowed miss
                throw OtpException.tooManyAttempts();
            }
            throw OtpException.invalid();
        }

        record.setUsedAt(now); // single use
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    // Only email after the DB transaction commits, so we never send a code that wasn't saved
    private void sendAfterCommit(String email, String name, String otp, OtpPurpose purpose) {
        final int expiry = expiryMinutes;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendOtpEmail(email, name, otp, purpose, expiry);
                }
            });
        } else {
            emailService.sendOtpEmail(email, name, otp, purpose, expiry);
        }
    }
}
