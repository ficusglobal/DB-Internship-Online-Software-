package internship_registration.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Student registration form.
 * Before calling this, verify the email:
 *   POST /api/auth/register/email/send-otp  ->  POST /api/auth/register/email/verify-otp
 * and send the returned emailVerificationToken here.
 *
 * Dropdown values:
 *   degree            : UG or PG                              (/api/public/master/degrees)
 *   department        : code such as BSC or MCOM              (/api/public/master/departments?degree=UG)
 *   semester          : 1-8 for UG, 1-4 for PG                (/api/public/master/semesters?degree=UG)
 *   academicSessionId : id from /api/public/master/academic-sessions
 *   departmentOther   : typed name, required only for UG_OTHERS / PG_OTHERS
 *   majorSubjectId    : id from /api/public/master/major-subjects
 */
@Data
public class StudentRegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name can be at most 150 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email can be at most 255 characters")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please provide a valid email, e.g. name@example.com")
    private String email;

    @NotBlank(message = "Please verify your email with the OTP before registering")
    private String emailVerificationToken;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit mobile number starting with 6, 7, 8 or 9")
    private String mobileNo;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotNull(message = "College is required")
    private Integer collegeId;

    @Size(max = 150, message = "Father's name can be at most 150 characters")
    private String fatherName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Degree is required (UG or PG)")
    private String degree;

    @NotBlank(message = "Department is required")
    private String department;

    // Required only when department is UG_OTHERS or PG_OTHERS: the department name typed by the student
    @Size(max = 150, message = "Department name can be at most 150 characters")
    private String departmentOther;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be between 1 and 8 (UG) or 1 and 4 (PG)")
    @Max(value = 8, message = "Semester must be between 1 and 8 (UG) or 1 and 4 (PG)")
    private Integer semester;

    @NotNull(message = "Academic session is required")
    private Integer academicSessionId;

    @NotNull(message = "Major subject is required")
    private Integer majorSubjectId;

    // Required: identifies the student (unique). Digits only.
    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "^[0-9]{1,20}$", message = "Registration number must contain digits only")
    private String registrationNumber;

    // Optional, but digits only when given
    @Pattern(regexp = "^$|^[0-9]{1,20}$", message = "College roll number must contain digits only")
    private String collegeRollNumber;

    @NotNull(message = "Internship batch is required")
    private Integer batchId;

    private Boolean termsAccepted;
}