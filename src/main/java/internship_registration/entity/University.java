package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "universities")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class University extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}