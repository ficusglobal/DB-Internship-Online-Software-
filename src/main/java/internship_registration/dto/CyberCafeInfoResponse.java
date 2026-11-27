package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CyberCafeInfoResponse {
    private String cyberCafeId;
    private String shopName;
    private String ownerName;
    private String mobileNo;
    private String email;
    private String address;
    private Boolean isVerified;
    private Boolean isActive;           // login account active
    private LocalDateTime joinedAt;

    // Only in GET /api/admin/cyber-cafes
    private Long totalStudents;
    private Long paidStudents;
    private Long pendingStudents;
    private Long cancelledStudents;
}