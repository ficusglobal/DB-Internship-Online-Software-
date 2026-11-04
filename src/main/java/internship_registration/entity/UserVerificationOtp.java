package internship_registration.entity;

import internship_registration.entity.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One row per issued OTP. Only the BCrypt hash of the code is stored, never the code itself.
 * Does NOT extend BaseEntity because the table has no created_by / updated_* columns.
 *
 * Two kinds of rows:
 *  - Existing account (email re-verification, password reset): user_id is set.
 *  - Before registration (no account yet): user_id is NULL, email is set. After the correct OTP a one-time
 *    token is issued (only its SHA-256 hash is stored in verification_token_hash); the registration call
 *    uses it once (token_used_at).
 */
@Entity
@Table(name = "user_verification_otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // users.id; NULL for an OTP sent before the account exists
    @Column(name = "user_id", length = 36)
    private String userId;

    // Set (lower-case) for an OTP sent before the account exists
    @Column(name = "email", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    // Failed verification attempts against this OTP
    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // The code was used (correct OTP), retired (new code requested) or burned (too many attempts)
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // SHA-256 (hex) of the token returned after a correct pre-registration OTP
    @Column(name = "verification_token_hash", length = 64, unique = true)
    private String verificationTokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    // Set when the token is used to create the account
    @Column(name = "token_used_at")
    private LocalDateTime tokenUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}