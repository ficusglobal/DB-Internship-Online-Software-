package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GET /api/admin/cyber-cafes/summary : the admin's cyber cafe numbers at a glance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CyberCafeOverviewResponse {

    private long totalCyberCafes;             // every cyber cafe account
    private long verifiedCyberCafes;          // is_verified = true
    private long activeCyberCafes;            // login account active
    private long cyberCafesWithStudents;      // registered at least one student
    private long cyberCafesWithoutStudents;   // registered nobody yet

    private Counts studentsByCyberCafes;      // registrations made by any cyber cafe
    private Counts selfRegisteredStudents;    // students who registered by themselves
    private Counts allStudents;               // both together

    /** total = paid + pending + cancelled */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counts {
        private long total;
        private long paid;       // ENROLLED, IN_PROGRESS, COMPLETED
        private long pending;    // REGISTERED, PAYMENT_PENDING
        private long cancelled;  // CANCELLED
    }
}