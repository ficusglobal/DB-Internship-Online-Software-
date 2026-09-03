package internship_registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StudentRegistrationRequest {

    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email format (e.g., name@example.com)")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNo;

    private String password;
    private String confirmPassword;

    private Integer collegeId;
    private String fatherName;
    private String gender;
    private String degree;
    private String department;
    private String academicSession;
    private String majorSubject;
    private String registrationNumber;
    private String collegeRollNumber;

    private Integer batchId;
    private Boolean termsAccepted;
}