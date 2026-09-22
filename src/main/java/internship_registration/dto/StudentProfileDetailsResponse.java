package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDetailsResponse {
    private String studentId;
    private String studentName;
    private String email;
    private String mobileNo;
    private String fatherName;
    private String gender;
    private String studentRegistrationNumber;
    private String collegeRollNumber;
    private String degree;
    private String department;
    private String academicSession;

    // College Details
    private Integer collegeId;
    private String collegeName;
    private String collegeCode;
    private String collegeAddress;
    private String city;
    private String state;
    private String pincode;

    // Internship / Batch Details
    private String registrationId;
    private String internshipRegistrationNumber;
    private String registrationStatus;
    private Integer batchId;
    private String batchName;
    private String courseName;
}