package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Returned after a correct OTP. Send emailVerificationToken in the student registration request. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationTokenResponse {
    private String message;
    private String email;
    private String emailVerificationToken;
    private Integer expiresInMinutes;
}