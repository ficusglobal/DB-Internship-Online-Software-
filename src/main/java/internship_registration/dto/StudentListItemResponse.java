package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the admin student list (one row per internship registration). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentListItemResponse {
    // ---- Registration
    private String registrationId;
    private String registrationStatus;   // REGISTERED, PAYMENT_PENDING, ENROLLED, IN_PROGRESS, COMPLETED, CANCELLED
    private String paymentStatus;        // PAID, PENDING or CANCELLED
    private BigDecimal payableAmount;
    private String consentStatus;
    private LocalDateTime registeredAt;
    private String registeredBy;         // SELF or CYBER_CAFE
    private String registeredByCyberCafe; // shop name (null when SELF); kept for older frontends
    private CyberCafeInfoResponse cyberCafe; // full cafe details (null when SELF)

    // ---- Student
    private String studentId;
    private String studentName;
    private boolean hasPhoto;
    private String photoUrl;             // GET with the JWT to show the photo; null when there is no photo
    private String email;
    private String mobileNo;
    private String fatherName;
    private String gender;
    private String registrationNumber;
    private String collegeRollNumber;
    private String degree;
    private String department;           // label, e.g. B.Sc
    private Integer semester;
    private String academicSession;
    private String majorSubject;

    // ---- College / University
    private Integer collegeId;
    private String collegeName;
    private String collegeCode;
    private Integer universityId;
    private String universityName;

    // ---- Internship
    private Integer batchId;
    private String batchName;
    private Integer courseId;
    private String courseName;
}