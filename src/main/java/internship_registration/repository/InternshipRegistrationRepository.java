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

    // Shared WHERE part of the admin / cyber cafe student list. Every filter is optional (null = skip it);
    // statuses is never empty. Text filters arrive already lower-cased and wrapped as %value%.
    String STUDENT_LIST_WHERE = "where r.status in :statuses "
            + "and (:collegeId is null or s.collegeId = :collegeId) "
            + "and (:universityId is null or s.collegeId in "
            + "     (select c.id from College c where c.university.id = :universityId)) "
            + "and (:batchId is null or r.batchId = :batchId) "
            + "and (:courseId is null or r.batchId in "
            + "     (select b.id from InternshipBatch b where b.course.id = :courseId)) "
            + "and (:name is null or lower(u.name) like :name escape '!') "
            + "and (:email is null or lower(u.email) like :email escape '!') "
            + "and (:mobile is null or u.mobileNo like :mobile escape '!') "
            + "and (:search is null or lower(u.name) like :search escape '!' "
            + "     or lower(u.email) like :search escape '!' or u.mobileNo like :search escape '!' "
            + "     or lower(cc.shopName) like :search escape '!') "
            + "and (:cyberCafeName is null or lower(cc.shopName) like :cyberCafeName escape '!' "
            + "     or lower(cc.ownerName) like :cyberCafeName escape '!') "
            + "and (:registeredBy is null "
            + "     or (:registeredBy = 'SELF' and cc.id is null) "
            + "     or (:registeredBy = 'CYBER_CAFE' and cc.id is not null)) "
            + "and (:cyberCafeId is null or cc.id = :cyberCafeId) ";

    // Admin student list (also used for a cyber cafe's own students, with cyberCafeId set)
    @Query(value = "select r from InternshipRegistration r "
            + "join fetch r.student s join fetch s.user u left join fetch r.cyberCafe cc "
            + STUDENT_LIST_WHERE
            + "order by r.createdAt desc",
            countQuery = "select count(r) from InternshipRegistration r "
                    + "join r.student s join s.user u left join r.cyberCafe cc "
                    + STUDENT_LIST_WHERE)
    Page<InternshipRegistration> searchForAdmin(@Param("universityId") Integer universityId,
                                                @Param("collegeId") Integer collegeId,
                                                @Param("courseId") Integer courseId,
                                                @Param("batchId") Integer batchId,
                                                @Param("statuses") Collection<RegistrationStatus> statuses,
                                                @Param("name") String name,
                                                @Param("email") String email,
                                                @Param("mobile") String mobile,
                                                @Param("search") String search,
                                                @Param("registeredBy") String registeredBy,
                                                @Param("cyberCafeId") String cyberCafeId,
                                                @Param("cyberCafeName") String cyberCafeName,
                                                Pageable pageable);

    // Registrations of one cyber cafe, counted per status: rows of [RegistrationStatus, Long]
    @Query("select r.status, count(r) from InternshipRegistration r "
            + "where r.cyberCafe.id = :cyberCafeId group by r.status")
    List<Object[]> countByStatusForCyberCafe(@Param("cyberCafeId") String cyberCafeId);

    // Every cyber cafe's registrations counted per status: rows of [cafeId, RegistrationStatus, Long]
    @Query("select r.cyberCafe.id, r.status, count(r) from InternshipRegistration r "
            + "where r.cyberCafe is not null group by r.cyberCafe.id, r.status")
    List<Object[]> countByCyberCafeAndStatus();

    // Students who registered by themselves (no cyber cafe), counted per status: rows of [RegistrationStatus, Long]
    @Query("select r.status, count(r) from InternshipRegistration r "
            + "where r.cyberCafe is null group by r.status")
    List<Object[]> countSelfRegisteredByStatus();
}