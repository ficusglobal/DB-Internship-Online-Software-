package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CyberCafeSummaryResponse {
    private String cyberCafeId;
    private String shopName;
    private String ownerName;
    private Boolean isVerified;

    private long totalStudents;      // every registration made by this cafe
    private long paidStudents;       // ENROLLED, IN_PROGRESS, COMPLETED
    private long pendingStudents;    // REGISTERED, PAYMENT_PENDING (payment not done yet)
    private long cancelledStudents;  // CANCELLED
}