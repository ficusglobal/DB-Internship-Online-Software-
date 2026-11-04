package internship_registration.controller;

import internship_registration.dto.PageResponse;
import internship_registration.dto.StudentListItemResponse;
import internship_registration.service.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SUPER_ADMIN (URL rule /api/admin/** in SecurityConfig).
 *
 * GET /api/admin/students?universityId=&collegeId=&courseId=&batchId=&status=&page=0&size=20
 *   status = ENROLLED   -> students who have paid
 *            REGISTERED -> registered, payment pending
 *            CANCELLED / ALL (default ALL)
 * All filters are optional and can be combined.
 */
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService adminStudentService;

    @GetMapping
    public ResponseEntity<PageResponse<StudentListItemResponse>> list(
            @RequestParam(required = false) Integer universityId,
            @RequestParam(required = false) Integer collegeId,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer batchId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminStudentService.list(universityId, collegeId, courseId, batchId, status, page, size));
    }
}