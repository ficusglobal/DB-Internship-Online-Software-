package internship_registration.repository;

import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.Student;
import internship_registration.entity.enums.ConsentStatus;
import internship_registration.entity.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipRegistrationRepository extends JpaRepository<InternshipRegistration, String> {

    Optional<InternshipRegistration> findFirstByStudent_IdOrderByCreatedAtDesc(String studentId);

    Optional<InternshipRegistration> findFirstByStudentOrderByCreatedAtDesc(Student student);

    // Used to block deleting a batch that already has registrations
    long countByBatchId(Integer batchId);

    // Admin: registrations that have a consent letter, newest first
    List<InternshipRegistration> findByConsentLetterUrlIsNotNullOrderByCreatedAtDesc();

    List<InternshipRegistration> findByConsentStatusAndConsentLetterUrlIsNotNullOrderByCreatedAtDesc(
            ConsentStatus consentStatus);

    // Students of one college in one batch who have paid (used to email the WhatsApp group link)
    @Query("select r from InternshipRegistration r "
            + "where r.batchId = :batchId and r.student.collegeId = :collegeId and r.status in :statuses")
    List<InternshipRegistration> findPaidByBatchAndCollege(@Param("batchId") Integer batchId,
                                                           @Param("collegeId") Integer collegeId,
                                                           @Param("statuses") Collection<RegistrationStatus> statuses);

    // Admin student list. Every filter is optional (pass null to skip it); statuses is never empty.
    @Query(value = "select r from InternshipRegistration r "
            + "join fetch r.student s join fetch s.user u left join fetch r.cyberCafe cc "
            + "where r.status in :statuses "
            + "and (:collegeId is null or s.collegeId = :collegeId) "
            + "and (:universityId is null or s.collegeId in "
            + "     (select c.id from College c where c.university.id = :universityId)) "
            + "and (:batchId is null or r.batchId = :batchId) "
            + "and (:courseId is null or r.batchId in "
            + "     (select b.id from InternshipBatch b where b.course.id = :courseId)) "
            + "order by r.createdAt desc",
            countQuery = "select count(r) from InternshipRegistration r join r.student s "
                    + "where r.status in :statuses "
                    + "and (:collegeId is null or s.collegeId = :collegeId) "
                    + "and (:universityId is null or s.collegeId in "
                    + "     (select c.id from College c where c.university.id = :universityId)) "
                    + "and (:batchId is null or r.batchId = :batchId) "
                    + "and (:courseId is null or r.batchId in "
                    + "     (select b.id from InternshipBatch b where b.course.id = :courseId))")
    Page<InternshipRegistration> searchForAdmin(@Param("universityId") Integer universityId,
                                                @Param("collegeId") Integer collegeId,
                                                @Param("courseId") Integer courseId,
                                                @Param("batchId") Integer batchId,
                                                @Param("statuses") Collection<RegistrationStatus> statuses,
                                                Pageable pageable);
}