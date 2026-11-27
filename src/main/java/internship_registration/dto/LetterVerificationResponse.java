package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterVerificationResponse {

    private boolean valid;               // true = genuine letter of an active (paid) registration
    private String status;               // VALID or CANCELLED
    private String message;

    // ---- Letter
    private String letterNumber;
    private LocalDateTime issuedAt;

    // ---- Student
    private String studentName;
    private String photoUrl;             // public photo address (no login); null when there is no photo
    private String mobileNo;             // masked by default, e.g. 98******10
    private String fatherName;
    private String gender;
    private String registrationNumber;   // university registration no.
    private String collegeRollNumber;
    private String degree;
    private String department;
    private Integer semester;
    private String academicSession;
    private String majorSubject;

    // ---- College / University
    private String collegeName;
    private String collegeCode;
    private String district;
    private String universityName;

    // ---- Internship
    private String courseName;
    private String batchName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long durationDays;           // startDate to endDate, both days counted
    private Integer theoryDurationHours;
    private Integer practicalDurationHours;
    private Integer reportPreparationHours;
    private Integer totalDurationHours;
    private String internshipStatus;     // ENROLLED, IN_PROGRESS, COMPLETED, CANCELLED
}