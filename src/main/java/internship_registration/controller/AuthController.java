package internship_registration.controller;

import internship_registration.dto.AuthResponse;
import internship_registration.dto.CyberCafeRegisterRequest;
import internship_registration.dto.LoginRequest;
import internship_registration.dto.StudentRegistrationRequest;
import internship_registration.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/student")
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        return ResponseEntity.ok(authService.registerStudent(request));
    }

    @PostMapping("/register/cybercafe")
    public ResponseEntity<AuthResponse> registerCyberCafe(@Valid @RequestBody CyberCafeRegisterRequest request) {
        return ResponseEntity.ok(authService.registerCyberCafe(request));
    }
}