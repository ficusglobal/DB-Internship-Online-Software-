package internship_registration.repository;

import internship_registration.entity.StudentAcceptanceLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAcceptanceLetterRepository extends JpaRepository<StudentAcceptanceLetter, Integer> {

    Optional<StudentAcceptanceLetter> findByRegistrationId(String registrationId);

    @Query("select coalesce(max(l.sequenceNumber), 0) from StudentAcceptanceLetter l " +
            "where l.collegeCode = :collegeCode and l.academicSession = :academicSession")
    Integer findMaxSequence(@Param("collegeCode") String collegeCode,
                            @Param("academicSession") String academicSession);

    @Query("select l from StudentAcceptanceLetter l " +
            "where (:collegeCode is null or l.collegeCode = :collegeCode) " +
            "and (:academicSession is null or l.academicSession = :academicSession) " +
            "order by l.generatedAt desc")
    List<StudentAcceptanceLetter> search(@Param("collegeCode") String collegeCode,
                                         @Param("academicSession") String academicSession);

    // QR code check (public)
    Optional<StudentAcceptanceLetter> findByVerificationCode(String verificationCode);

    // Gives an old letter its QR code once. "is null" makes sure two parallel requests cannot
    // both set a code (the second one changes 0 rows and then reads the first one's code).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update StudentAcceptanceLetter l set l.verificationCode = :code "
            + "where l.id = :id and l.verificationCode is null")
    int setVerificationCodeIfMissing(@Param("id") Integer id, @Param("code") String code);
}