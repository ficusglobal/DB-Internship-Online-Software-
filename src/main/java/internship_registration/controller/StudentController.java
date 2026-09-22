package internship_registration.controller;

import internship_registration.dto.StudentProfileDetailsResponse;
import internship_registration.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;

    // 1. Logged-in student fetches their own profile using JWT (extracts User ID from Auth)
    @GetMapping("/me")
    public ResponseEntity<StudentProfileDetailsResponse> getMyDetails(Authentication authentication) {
        String loggedInUserId = authentication.getName();
        return ResponseEntity.ok(studentService.getStudentDetails(loggedInUserId));
    }

    // 2. Lookup any student by ID (for Admin or Cyber Cafe verification)
    @GetMapping("/{id}")
    public ResponseEntity<StudentProfileDetailsResponse> getStudentById(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getStudentDetails(id));
    }
}