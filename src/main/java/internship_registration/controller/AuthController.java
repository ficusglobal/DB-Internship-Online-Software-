package internship_registration.controller;

import internship_registration.dto.AuthResponse;
import internship_registration.dto.CyberCafeRegisterRequest;
import internship_registration.dto.LoginRequest;
import internship_registration.dto.StudentRegistrationRequest;
import internship_registration.service.AuthService;
import internship_registration.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/student")
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        AuthResponse response = authService.registerStudent(request);
        sendVerificationEmailQuietly(response.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/cybercafe")
    public ResponseEntity<AuthResponse> registerCyberCafe(@Valid @RequestBody CyberCafeRegisterRequest request) {
        AuthResponse response = authService.registerCyberCafe(request);
        sendVerificationEmailQuietly(response.getEmail());
        return ResponseEntity.ok(response);
    }

    // The account already exists at this point, so a mail problem must never fail the registration.
    // The user can request another code from POST /api/auth/email/send-otp.
    private void sendVerificationEmailQuietly(String email) {
        try {
            otpService.sendEmailVerificationOtp(email);
        } catch (Exception e) {
            log.warn("Could not queue the email verification OTP: {}", e.getMessage());
        }
    }
}
