package internship_registration.repository;

import internship_registration.entity.CyberCafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CyberCafeRepository extends JpaRepository<CyberCafe, String> {
    Optional<CyberCafe> findByUserId(String userId);
}