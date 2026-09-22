package internship_registration.repository;

import internship_registration.entity.Student;
import internship_registration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByUser_Id(String userId);

    Optional<Student> findByUser(User user);
}