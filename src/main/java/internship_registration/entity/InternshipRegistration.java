package internship_registration.entity;

import internship_registration.entity.enums.ConsentStatus;
import internship_registration.entity.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Builder.Default
    private Boolean termsAccepted = true;

    // CONSENT WORKFLOW
    @Column(name = "consent_letter_url", length = 1000)
    private String consentLetterUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status")
    @Builder.Default
    private ConsentStatus consentStatus = ConsentStatus.PENDING;

    // Why the admin rejected the consent letter (shown to the student); cleared when a new file is uploaded
    @Column(name = "consent_rejection_reason", length = 500)
    private String consentRejectionReason;

    // When the admin last approved or rejected the consent letter
    @Column(name = "consent_reviewed_at")
    private LocalDateTime consentReviewedAt;

    // When the WhatsApp group link was emailed to the student (NULL = not sent yet)
    @Column(name = "whatsapp_link_sent_at")
    private LocalDateTime whatsappLinkSentAt;

    // UPDATED: Using your provided enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
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