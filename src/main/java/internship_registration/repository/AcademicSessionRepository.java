package internship_registration.repository;

import internship_registration.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Integer> {

    // Dropdown: active sessions, newest first ("2025-2028" sorts after "2023-2027")
    List<AcademicSession> findByIsActiveTrueOrderByNameDesc();

    // Admin list: all sessions, newest first
    List<AcademicSession> findAllByOrderByNameDesc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}