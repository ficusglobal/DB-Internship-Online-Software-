package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "internship_courses")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "theory_duration_hours", nullable = false)
    private Integer theoryDurationHours = 0;

    @Column(name = "practical_duration_hours", nullable = false)
    private Integer practicalDurationHours = 0;

    @Column(name = "report_preparation_hours", nullable = false)
    private Integer reportPreparationHours = 0;

    @Column(name = "total_duration_hours", nullable = false)
    private Integer totalDurationHours = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}