package internship_registration.entity;

import internship_registration.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Read-only mapping for your INT AUTO_INCREMENT column
    @Column(name = "serial_no", insertable = false, updatable = false)
    private Integer serialNo;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "college_id", nullable = false)
    private Integer collegeId;

    @Column(name = "father_name", length = 150)
    private String fatherName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "registration_number", length = 100, unique = true)
    private String registrationNumber;

    @Column(name = "college_roll_number", length = 100)
    private String collegeRollNumber;

    @Column(nullable = false, length = 100)
    private String degree;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "academic_session", nullable = false, length = 20)
    private String academicSession;

    @Column(name = "major_subject", length = 150)
    private String majorSubject;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}