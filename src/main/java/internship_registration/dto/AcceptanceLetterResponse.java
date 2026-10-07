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
public class AcceptanceLetterResponse {

    // ---- Letter
    private Integer id;
    private String letterNumber;
    private Integer sequenceNumber;
    private LocalDateTime generatedAt;

    // ---- Student
    private String registrationId;
    private String studentId;
    private String studentName;
    private String fatherName;
    private String gender;
    private String email;
    private String mobileNo;
    private String studentRegistrationNumber;   // university registration no. entered by the student
    private String collegeRollNumber;
    private String degree;
    private String department;
    private String majorSubject;
    private String academicSession;

    // ---- College / University
    private String collegeCode;
    private String collegeName;
    private String collegeAddress;               // full address on one line
    private String city;
    private String state;
    private String pincode;
    private String district;
    private String universityName;
    private String universityCode;

    // ---- Internship
    private String internshipRegistrationNumber;
    private String registrationStatus;
    private String batchName;
    private LocalDate batchStartDate;
    private LocalDate batchEndDate;
    private String courseName;
    private String courseCode;
    private Integer theoryDurationHours;
    private Integer practicalDurationHours;
    private Integer reportPreparationHours;
    private Integer totalDurationHours;
}