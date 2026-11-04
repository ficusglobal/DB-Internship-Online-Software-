package internship_registration.repository;

import internship_registration.entity.Student;
import internship_registration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByUser_Id(String userId);

    Optional<Student> findByUser(User user);

    // Used to block deleting a college that still has students
    long countByCollegeId(Integer collegeId);

    // Duplicate checks at registration: registration no. is unique overall, roll no. is unique within a college
    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByCollegeIdAndCollegeRollNumber(Integer collegeId, String collegeRollNumber);

    // Students using an academic session (by id, or by name for older rows saved before academic_session_id)
    @Query("select count(s) from Student s where s.academicSessionId = :id or s.academicSession = :name")
    long countUsingAcademicSession(@Param("id") Integer id, @Param("name") String name);

    // Students using a major subject (by id, or by name for older rows saved before major_subject_id)
    @Query("select count(s) from Student s where s.majorSubjectId = :id or s.majorSubject = :name")
    long countUsingMajorSubject(@Param("id") Integer id, @Param("name") String name);
}