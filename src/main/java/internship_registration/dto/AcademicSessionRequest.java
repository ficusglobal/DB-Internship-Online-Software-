package internship_registration.dto;

import lombok.Data;

/** Create / update an academic session. name must look like 2023-2027. */
@Data
public class AcademicSessionRequest {
    private String name;
    private Boolean active;
}