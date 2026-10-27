package internship_registration.repository;

import internship_registration.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollegeRepository extends JpaRepository<College, Integer> {

    // Required for the cascading dropdown in the UI
    List<College> findByUniversityIdAndDistrictIdAndIsActiveTrue(Integer universityId, Integer districtId);

    // Duplicate checks (case-insensitive)
    boolean existsByUniversityIdAndDistrictIdAndNameIgnoreCase(Integer universityId, Integer districtId, String name);

    boolean existsByCodeIgnoreCase(String code);

    // Same checks for updates: ignore the record being edited
    boolean existsByUniversityIdAndDistrictIdAndNameIgnoreCaseAndIdNot(
            Integer universityId, Integer districtId, String name, Integer id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    // Admin lists (active and inactive)
    List<College> findByUniversityId(Integer universityId);

    List<College> findByDistrictId(Integer districtId);

    List<College> findByUniversityIdAndDistrictId(Integer universityId, Integer districtId);

    // Used to block deleting a university or district that still has colleges
    long countByUniversityId(Integer universityId);

    long countByDistrictId(Integer districtId);
}