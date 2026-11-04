package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

/** Academic session such as "2023-2027" (shown in the registration dropdown). Table: academic_sessions */
@Entity
@Table(name = "academic_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // e.g. 2023-2027 (unique)
    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}