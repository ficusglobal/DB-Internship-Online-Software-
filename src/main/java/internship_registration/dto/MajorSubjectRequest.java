package internship_registration.dto;

import lombok.Data;

/** Create / update a major subject. Names are unique. */
@Data
public class MajorSubjectRequest {
    private String name;
    private Boolean active;
}