package internship_registration.controller;

import internship_registration.dto.AuthResponse;
import internship_registration.dto.RegisterRequest;
import internship_registration.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminUserController {

    private final AuthService authService;


    @PostMapping("/create")
    public ResponseEntity<AuthResponse> createAdmin(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.createUniversityAdmin(request));
    }
}