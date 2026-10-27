package internship_registration.dto;

import internship_registration.entity.enums.ConsentStatus;
import lombok.Data;

@Data
public class ConsentReviewRequest {
    private ConsentStatus status;

    // Required when the status is REJECTED: tells the student what to fix (max 500 characters)
    private String reason;
}