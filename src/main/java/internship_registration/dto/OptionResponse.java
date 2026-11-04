package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One dropdown option. code = the value to send back to the API, label = the text to show.
 * degree / maxSemester are filled only where they make sense (null fields are not sent).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {
    private String code;
    private String label;
    private String degree;
    private Integer maxSemester;
}