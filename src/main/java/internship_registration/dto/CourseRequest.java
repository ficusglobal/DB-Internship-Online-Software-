package internship_registration.dto;

import lombok.Data;

@Data
public class CourseRequest {
    private String name;
    private String code;
    private String description;
    private Integer theoryDurationHours;
    private Integer practicalDurationHours;
    private Integer reportPreparationHours;
    private Integer totalDurationHours;
}