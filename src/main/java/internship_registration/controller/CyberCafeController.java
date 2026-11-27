package internship_registration.controller;

import internship_registration.dto.CyberCafeSummaryResponse;
import internship_registration.dto.PageResponse;
import internship_registration.dto.StudentListFilter;
import internship_registration.dto.StudentListItemResponse;
import internship_registration.service.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/cyber-cafe")
@RequiredArgsConstructor
public class CyberCafeController {

    private final AdminStudentService studentListService;

    @GetMapping("/summary")
    public ResponseEntity<CyberCafeSummaryResponse> summary(Authentication authentication) {
        return ResponseEntity.ok(studentListService.cafeSummary(authentication.getName()));
    }

    @GetMapping("/students")
    public ResponseEntity<PageResponse<StudentListItemResponse>> students(
            @ModelAttribute StudentListFilter filter, Authentication authentication) {
        return ResponseEntity.ok(studentListService.cafeStudents(authentication.getName(), filter));
    }
}