package internship_registration.controller;

import internship_registration.dto.CreateOrderRequest;
import internship_registration.dto.CreateOrderResponse;
import internship_registration.dto.PaymentVerificationRequest;
import internship_registration.dto.PaymentVerificationResponse;
import internship_registration.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request, Authentication authentication) throws Exception {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("SUPER_ADMIN") || a.equals("UNIVERSITY_ADMIN"));
        CreateOrderResponse response = paymentService.createOrder(request, authentication.getName(), isAdmin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        PaymentVerificationResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            paymentService.handleWebhookEvent(rawPayload, signature);
            return ResponseEntity.ok("Webhook received and processed");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Webhook processing failure");
        }
    }
}