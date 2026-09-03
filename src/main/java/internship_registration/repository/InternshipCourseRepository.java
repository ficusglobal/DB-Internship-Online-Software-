package internship_registration.repository;
import internship_registration.entity.InternshipCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipCourseRepository extends JpaRepository<InternshipCourse, Integer> {
    List<InternshipCourse> findAllByIsActiveTrue();
}