package internship_registration.controller;

import internship_registration.dto.MessageResponse;
import internship_registration.dto.ResetPasswordRequest;
import internship_registration.dto.SendOtpRequest;
import internship_registration.dto.VerifyEmailOtpRequest;
import internship_registration.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Lives under /api/auth/**, which SecurityConfig already permits without a JWT.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OtpController {

    private static final String GENERIC_SENT =
            "If an eligible account exists for this email, a 6-digit code has been sent. "
                    + "You can request a new code after 60 seconds.";

    private final OtpService otpService;

    @PostMapping("/email/send-otp")
    public ResponseEntity<MessageResponse> sendEmailVerificationOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendEmailVerificationOtp(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(GENERIC_SENT));
    }

    @PostMapping("/email/verify-otp")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailOtpRequest request) {
        otpService.verifyEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(new MessageResponse("Email verified successfully"));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(GENERIC_SENT));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        otpService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully. Please log in."));
    }
}
