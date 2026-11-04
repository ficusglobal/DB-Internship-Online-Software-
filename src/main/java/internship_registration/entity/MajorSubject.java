package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

/** Major subject such as "Physics" (shown in the registration dropdown). Table: major_subjects */
@Entity
@Table(name = "major_subjects")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorSubject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Unique name
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}