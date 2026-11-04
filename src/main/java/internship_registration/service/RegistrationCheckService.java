package internship_registration.service;

import internship_registration.dto.RegistrationCheckResponse;
import internship_registration.exception.DuplicateResourceException;
import internship_registration.repository.StudentRepository;
import internship_registration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Duplicate checks for the student registration form.
 *  - email and mobile no.  : unique across all users
 *  - registration no.      : unique across all students (digits only)
 *  - college roll no.      : unique within the same college (digits only)
 * Used live by the form (GET /api/auth/register/check) and again when the form is submitted.
 */
@Service
@RequiredArgsConstructor
public class RegistrationCheckService {

    public static final Pattern MOBILE = Pattern.compile("^[0-9]{10}$");
    public static final Pattern DIGITS = Pattern.compile("^[0-9]{1,20}$");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /** Checks only the values that were sent. Throws IllegalArgumentException (400) for a wrong format. */
    @Transactional(readOnly = true)
    public RegistrationCheckResponse check(String email, String mobileNo, String registrationNumber,
                                           String collegeRollNumber, Integer collegeId) {
        Map<String, String> messages = new LinkedHashMap<>();
        RegistrationCheckResponse.RegistrationCheckResponseBuilder result = RegistrationCheckResponse.builder();

        if (hasText(email)) {
            boolean exists = userRepository.existsByEmailIgnoreCase(email.trim());
            result.emailExists(exists);
            if (exists) messages.put("email", "This email is already registered");
        }
        if (hasText(mobileNo)) {
            String mobile = mobileNo.trim();
            if (!MOBILE.matcher(mobile).matches()) {
                throw new IllegalArgumentException("Mobile number must be exactly 10 digits");
            }
            boolean exists = userRepository.existsByMobileNo(mobile);
            result.mobileNoExists(exists);
            if (exists) messages.put("mobileNo", "This mobile number is already registered");
        }
        if (hasText(registrationNumber)) {
            String regNo = digitsOnly(registrationNumber, "Registration number");
            boolean exists = studentRepository.existsByRegistrationNumber(regNo);
            result.registrationNumberExists(exists);
            if (exists) messages.put("registrationNumber", "This registration number is already registered");
        }
        if (hasText(collegeRollNumber)) {
            String rollNo = digitsOnly(collegeRollNumber, "College roll number");
            if (collegeId == null) {
                throw new IllegalArgumentException("Select the college first to check the college roll number");
            }
            boolean exists = studentRepository.existsByCollegeIdAndCollegeRollNumber(collegeId, rollNo);
            result.collegeRollNumberExists(exists);
            if (exists) messages.put("collegeRollNumber", "This roll number is already registered in this college");
        }

        return result.available(messages.isEmpty()).messages(messages).build();
    }

    /** Same checks at submit time: throws DuplicateResourceException (HTTP 409) on the first value already used. */
    @Transactional(readOnly = true)
    public void assertAvailable(String email, String mobileNo, String registrationNumber,
                                String collegeRollNumber, Integer collegeId) {
        RegistrationCheckResponse r = check(email, mobileNo, registrationNumber, collegeRollNumber, collegeId);
        if (!r.isAvailable()) {
            throw new DuplicateResourceException(r.getMessages().values().iterator().next());
        }
    }

    /** Registration no. / roll no.: digits only, 1 to 20 of them. */
    public static String digitsOnly(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (!DIGITS.matcher(v).matches()) {
            throw new IllegalArgumentException(label + " must contain digits only (0-9), at most 20");
        }
        return v;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}