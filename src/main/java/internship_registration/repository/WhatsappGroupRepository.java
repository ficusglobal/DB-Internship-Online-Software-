package internship_registration.repository;

import internship_registration.entity.WhatsappGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsappGroupRepository extends JpaRepository<WhatsappGroup, Integer> {

    Optional<WhatsappGroup> findByCollegeIdAndBatchId(Integer collegeId, Integer batchId);

    List<WhatsappGroup> findByCollegeId(Integer collegeId);

    List<WhatsappGroup> findByBatchId(Integer batchId);

    boolean existsByCollegeIdAndBatchId(Integer collegeId, Integer batchId);

    boolean existsByCollegeIdAndBatchIdAndIdNot(Integer collegeId, Integer batchId, Integer id);


    long countByCollegeId(Integer collegeId);

    long countByBatchId(Integer batchId);
}