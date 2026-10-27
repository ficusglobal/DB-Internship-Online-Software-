package internship_registration.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchUpdateRequest {
    private Integer courseId;
    private String batchName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal fee;
    private Integer maxStudents;
    private String status;
    private Boolean isActive;
}