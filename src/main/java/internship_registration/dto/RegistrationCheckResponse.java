package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of the live duplicate check on the registration form.
 * A field is true when that value is ALREADY USED; fields that were not sent are left out.
 * available = true when none of the sent values is used. messages = field -> text to show under the input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationCheckResponse {
    private Boolean emailExists;
    private Boolean mobileNoExists;
    private Boolean registrationNumberExists;
    private Boolean collegeRollNumberExists;
    private boolean available;
    private Map<String, String> messages;
}