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
}