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

    // University registration no. (digits only, unique across students)
    @Column(name = "registration_number", length = 100, unique = true)
    private String registrationNumber;

    // College roll no. (digits only, unique within a college)
    @Column(name = "college_roll_number", length = 100)
    private String collegeRollNumber;

    // UG or PG
    @Column(nullable = false, length = 100)
    private String degree;

    // Department code: BA, BSC, BCOM, BAGRI, UG_OTHERS, MA, MSC, MCOM, MAGRI, PG_OTHERS
    @Column(nullable = false, length = 100)
    private String department;

    // Department name typed by the student when department is UG_OTHERS / PG_OTHERS
    @Column(name = "department_other", length = 150)
    private String departmentOther;

    // 1-8 for UG, 1-4 for PG (nullable in the database only for students registered before this field existed)
    @Column(name = "semester")
    private Integer semester;

    // academic_sessions.id
    @Column(name = "academic_session_id")
    private Integer academicSessionId;

    // Session name, e.g. 2023-2027 (copied from academic_sessions; also used in the acceptance letter number)
    @Column(name = "academic_session", nullable = false, length = 20)
    private String academicSession;

    // major_subjects.id
    @Column(name = "major_subject_id")
    private Integer majorSubjectId;

    // Subject name (copied from major_subjects)
    @Column(name = "major_subject", length = 150)
    private String majorSubject;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}