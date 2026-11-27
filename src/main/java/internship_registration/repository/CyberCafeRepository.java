package internship_registration.repository;

import internship_registration.entity.CyberCafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CyberCafeRepository extends JpaRepository<CyberCafe, String> {

    Optional<CyberCafe> findByUserId(String userId);

    // Admin list / dropdown of cyber cafes, with the owner's login account
    @Query("select c from CyberCafe c join fetch c.user u order by c.shopName")
    List<CyberCafe> findAllWithUser();

    // One cafe with its login account (admin cafe details)
    @Query("select c from CyberCafe c join fetch c.user u where c.id = :id")
    Optional<CyberCafe> findByIdWithUser(@Param("id") String id);
}