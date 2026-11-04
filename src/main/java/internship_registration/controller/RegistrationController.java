package internship_registration.controller;

import internship_registration.dto.EmailVerificationTokenResponse;
import internship_registration.dto.MessageResponse;
import internship_registration.dto.RegistrationCheckResponse;
import internship_registration.dto.SendOtpRequest;
import internship_registration.dto.VerifyEmailOtpRequest;
import internship_registration.service.RegistrationCheckService;
import internship_registration.service.RegistrationEmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Steps BEFORE the student account is created. Lives under /api/auth/**, which needs no login.
 *  1. POST /api/auth/register/email/send-otp     { "email" }
 *  2. POST /api/auth/register/email/verify-otp   { "email", "otp" }  -> emailVerificationToken
 *  3. GET  /api/auth/register/check?...           live duplicate check while the form is filled
 *  4. POST /api/auth/register/student             (AuthController) with emailVerificationToken
 */
@RestController
@RequestMapping("/api/auth/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationEmailVerificationService emailVerificationService;
    private final RegistrationCheckService checkService;

    @PostMapping("/email/send-otp")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        emailVerificationService.sendOtp(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(
                "A 6-digit code has been sent to your email. You can request a new code after 60 seconds."));
    }

    @PostMapping("/email/verify-otp")
    public ResponseEntity<EmailVerificationTokenResponse> verifyOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(emailVerificationService.verifyOtp(request.getEmail(), request.getOtp()));
    }

    // Example: /api/auth/register/check?mobileNo=9876543210&registrationNumber=123456&collegeRollNumber=42&collegeId=3
    @GetMapping("/check")
    public ResponseEntity<RegistrationCheckResponse> check(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobileNo,
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String collegeRollNumber,
            @RequestParam(required = false) Integer collegeId) {
        return ResponseEntity.ok(checkService.check(email, mobileNo, registrationNumber, collegeRollNumber, collegeId));
    }
}