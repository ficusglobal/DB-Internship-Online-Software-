package internship_registration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsappGroupResponse {
    private Integer id;
    private Integer collegeId;
    private String collegeName;
    private String collegeCode;
    private Integer batchId;
    private String batchName;
    private String courseName;
    private String groupName;
    private String groupUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}