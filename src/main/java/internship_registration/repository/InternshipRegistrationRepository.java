package internship_registration.repository;

import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InternshipRegistrationRepository extends JpaRepository<InternshipRegistration, String> {

    Optional<InternshipRegistration> findFirstByStudent_IdOrderByCreatedAtDesc(String studentId);

    Optional<InternshipRegistration> findFirstByStudentOrderByCreatedAtDesc(Student student);
}