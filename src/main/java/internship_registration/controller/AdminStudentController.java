package internship_registration.controller;

import internship_registration.dto.CyberCafeInfoResponse;
import internship_registration.dto.CyberCafeOverviewResponse;
import internship_registration.dto.PageResponse;
import internship_registration.dto.StudentListFilter;
import internship_registration.dto.StudentListItemResponse;
import internship_registration.service.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService adminStudentService;

    @GetMapping("/students")
    public ResponseEntity<PageResponse<StudentListItemResponse>> list(@ModelAttribute StudentListFilter filter) {
        return ResponseEntity.ok(adminStudentService.list(filter));
    }

    @GetMapping("/cyber-cafes")
    public ResponseEntity<List<CyberCafeInfoResponse>> cyberCafes(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminStudentService.listCyberCafes(search));
    }

    @GetMapping("/cyber-cafes/summary")
    public ResponseEntity<CyberCafeOverviewResponse> cyberCafeSummary() {
        return ResponseEntity.ok(adminStudentService.cyberCafeOverview());
    }

    @GetMapping("/cyber-cafes/{id}")
    public ResponseEntity<CyberCafeInfoResponse> cyberCafe(@PathVariable String id) {
        return ResponseEntity.ok(adminStudentService.cyberCafeDetails(id));
    }
}