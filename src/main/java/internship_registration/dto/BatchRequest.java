package internship_registration.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    @NotNull(message = "Batch name is required")
    private String batchName;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Fee is required")
    private BigDecimal fee;

    private Integer maxStudents;
}