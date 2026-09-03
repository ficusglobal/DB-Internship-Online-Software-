package internship_registration.repository;
import internship_registration.entity.InternshipBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternshipBatchRepository extends JpaRepository<InternshipBatch, Integer> {
    List<InternshipBatch> findByCourseIdAndIsActiveTrue(Integer courseId);
}