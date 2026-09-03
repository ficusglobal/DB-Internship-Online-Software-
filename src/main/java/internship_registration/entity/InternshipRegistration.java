package internship_registration.entity;

import internship_registration.entity.enums.ConsentStatus;
import internship_registration.entity.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "internship_registrations")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipRegistration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "cyber_cafe_id")
    private CyberCafe cyberCafe;

    @Column(name = "batch_id", nullable = false)
    private Integer batchId;

    @Column(name = "registration_number", length = 100, unique = true)
    private String registrationNumber;

    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted = true;

    // CONSENT WORKFLOW
    @Column(name = "consent_letter_url", length = 1000)
    private String consentLetterUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status")
    private ConsentStatus consentStatus = ConsentStatus.PENDING;

    // UPDATED: Using your provided enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @Column(name = "course_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal courseFee;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "payable_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payableAmount;

    @Column(name = "is_certificate_provided", nullable = false)
    private Boolean isCertificateProvided;
}