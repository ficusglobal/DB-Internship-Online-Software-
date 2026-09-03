package internship_registration.repository;

import internship_registration.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UniversityRepository extends JpaRepository<University, Integer> {

    // Required to populate the University dropdown
    List<University> findAllByIsActiveTrue();
}