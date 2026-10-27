package internship_registration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import internship_registration.entity.enums.ConsentStatus;
import internship_registration.entity.enums.RegistrationStatus;
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
public class ConsentSummaryResponse {
    private String registrationId;
    private String studentName;
    private String email;
    private String mobileNo;
    private Integer collegeId;
    private Integer batchId;
    private RegistrationStatus registrationStatus;
    private ConsentStatus consentStatus;
    private Boolean consentUploaded;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
}