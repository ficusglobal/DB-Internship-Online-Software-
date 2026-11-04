package internship_registration.repository;

import internship_registration.entity.MajorSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MajorSubjectRepository extends JpaRepository<MajorSubject, Integer> {

    // Dropdown: active subjects A-Z
    List<MajorSubject> findByIsActiveTrueOrderByNameAsc();

    // Admin list: all subjects A-Z
    List<MajorSubject> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}