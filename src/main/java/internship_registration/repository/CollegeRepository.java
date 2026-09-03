package internship_registration.repository;

import internship_registration.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollegeRepository extends JpaRepository<College, Integer> {

    // Required for the cascading dropdown in the UI
    List<College> findByUniversityIdAndDistrictIdAndIsActiveTrue(Integer universityId, Integer districtId);
}