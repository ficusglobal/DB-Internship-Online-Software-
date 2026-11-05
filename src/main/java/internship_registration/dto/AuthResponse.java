package internship_registration.dto;

import internship_registration.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by login and registration.
 * studentId / registrationId / registrationStatus are filled only for a STUDENT account
 * (registration = the student's latest internship registration); for other roles they are left out of the JSON.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String userId;
    private String name;
    private String email;
    private Role role;

    // ---- STUDENT only
    private String studentId;
    private String registrationId;
    private String registrationStatus;
}