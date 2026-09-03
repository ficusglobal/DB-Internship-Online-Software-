package internship_registration.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "internship_batches")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonPropertyOrder({
        "id", "batchName", "startDate", "endDate", "registrationStartDate",
        "registrationEndDate", "fee", "maxStudents", "status", "isActive",
        "createdAt", "updatedAt", "course"
})
public class InternshipBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"createdAt", "updatedAt", "createdBy", "updatedBy"})
    private InternshipCourse course;

    @Column(name = "batch_name", nullable = false, length = 255)
    private String batchName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "registration_start_date")
    private LocalDateTime registrationStartDate;

    @Column(name = "registration_end_date")
    private LocalDateTime registrationEndDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}