package internship_registration.repository;

import internship_registration.entity.UserVerificationOtp;
import internship_registration.entity.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserVerificationOtpRepository extends JpaRepository<UserVerificationOtp, Integer> {

    // Row lock so two concurrent verify calls cannot both consume the same OTP
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserVerificationOtp> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
            String userId, OtpPurpose purpose);

    // Latest OTP of any state, used for the resend cooldown
    Optional<UserVerificationOtp> findFirstByUserIdAndPurposeOrderByCreatedAtDescIdDesc(
            String userId, OtpPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(String userId, OtpPurpose purpose, LocalDateTime after);

    // Retire older unused OTPs so only the newest one is ever valid
    @Modifying(flushAutomatically = true)
    @Query("update UserVerificationOtp o set o.usedAt = :now " +
            "where o.userId = :userId and o.purpose = :purpose and o.usedAt is null")
    int invalidateActive(@Param("userId") String userId,
                         @Param("purpose") OtpPurpose purpose,
                         @Param("now") LocalDateTime now);

    // =====================================================================
    // BEFORE REGISTRATION (no account yet): rows have user_id NULL and email set
    // =====================================================================

    // Latest code that can still be entered (row lock so two verify calls can't both use it)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserVerificationOtp> findFirstByEmailAndUserIdIsNullAndPurposeAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
            String email, OtpPurpose purpose);

    // Latest row of any state, for the resend cooldown
    Optional<UserVerificationOtp> findFirstByEmailAndUserIdIsNullAndPurposeOrderByCreatedAtDescIdDesc(
            String email, OtpPurpose purpose);

    long countByEmailAndUserIdIsNullAndPurposeAndCreatedAtAfter(String email, OtpPurpose purpose, LocalDateTime after);

    // Token presented by the registration form
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserVerificationOtp> findByVerificationTokenHash(String verificationTokenHash);

    // New code requested: retire the older unused codes of this email
    @Modifying(flushAutomatically = true)
    @Query("update UserVerificationOtp o set o.usedAt = :now " +
            "where o.email = :email and o.userId is null and o.purpose = :purpose and o.usedAt is null")
    int invalidateActiveByEmail(@Param("email") String email,
                                @Param("purpose") OtpPurpose purpose,
                                @Param("now") LocalDateTime now);

    // New code requested: older verification tokens of this email can no longer be used
    @Modifying(flushAutomatically = true)
    @Query("update UserVerificationOtp o set o.tokenUsedAt = :now " +
            "where o.email = :email and o.userId is null and o.purpose = :purpose " +
            "and o.verificationTokenHash is not null and o.tokenUsedAt is null")
    int retireTokensByEmail(@Param("email") String email,
                            @Param("purpose") OtpPurpose purpose,
                            @Param("now") LocalDateTime now);
}