package internship_registration.dto;

import lombok.Data;

@Data
public class WhatsappGroupRequest {
    private Integer collegeId;
    private Integer batchId;
    private String groupName;
    private String groupUrl;
    private Boolean isActive;
}