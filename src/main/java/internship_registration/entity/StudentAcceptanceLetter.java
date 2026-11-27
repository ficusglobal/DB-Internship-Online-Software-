package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Does NOT extend BaseEntity because the table only has generated_at (no audit columns).
 */
@Entity
@Table(name = "student_acceptance_letter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAcceptanceLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // internship_registrations.id is a UUID string in this project
    @Column(name = "registration_id", nullable = false, length = 36)
    private String registrationId;

    @Column(name = "college_code", nullable = false, length = 30)
    private String collegeCode;

    @Column(name = "academic_session", nullable = false, length = 20)
    private String academicSession;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "letter_number", nullable = false, length = 100)
    private String letterNumber;

    // Random secret printed inside the QR code (32 hex characters). Not the letter number, because the
    // letter number is sequential and anyone could guess the next one and read other students' details.
    // NULL for letters made before this column existed; filled the first time the letter is opened.
    @Column(name = "verification_code", length = 64, unique = true)
    private String verificationCode;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}