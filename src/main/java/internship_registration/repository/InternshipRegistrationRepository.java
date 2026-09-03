package internship_registration.repository;

import internship_registration.entity.InternshipRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternshipRegistrationRepository extends JpaRepository<InternshipRegistration, String> {
}