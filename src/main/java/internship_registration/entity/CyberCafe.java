package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cyber_cafes")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CyberCafe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "shop_name", nullable = false, length = 255)
    private String shopName;

    @Column(name = "owner_name", nullable = false, length = 150)
    private String ownerName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;
}