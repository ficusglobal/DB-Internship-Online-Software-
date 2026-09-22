package internship_registration.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import internship_registration.dto.CreateOrderRequest;
import internship_registration.dto.CreateOrderResponse;
import internship_registration.dto.PaymentVerificationRequest;
import internship_registration.dto.PaymentVerificationResponse;
import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.Payment;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.repository.InternshipRegistrationRepository;
import internship_registration.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final InternshipRegistrationRepository registrationRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) throws Exception {
        InternshipRegistration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + request.getRegistrationId()));

        BigDecimal amount = BigDecimal.valueOf(5000.00);
        String currency = "INR";
        String receiptNumber = "RCPT-" + System.currentTimeMillis();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receiptNumber);

        JSONObject notes = new JSONObject();
        notes.put("registrationId", registration.getId());
        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = order.get("id");

        Payment payment = Payment.builder()
                .registrationId(registration.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(amount)
                .currency(currency)
                .receiptNumber(receiptNumber)
                .status("CREATED")
                .build();

        paymentRepository.save(payment);

        return CreateOrderResponse.builder()
                .orderId(razorpayOrderId)
                .amount(amount)
                .currency(currency)
                .razorpayKeyId(keyId)
                .receiptNumber(receiptNumber)
                .registrationId(registration.getId())
                .build();
    }

    @Transactional
    public PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (!isValid) {
                return PaymentVerificationResponse.builder()
                        .status("FAILED")
                        .verified(false)
                        .message("Invalid payment signature")
                        .build();
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment record not found for order: " + request.getRazorpayOrderId()));

            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus("PAID");
            paymentRepository.save(payment);

            updateRegistrationConfirmed(payment.getRegistrationId());

            return PaymentVerificationResponse.builder()
                    .status("SUCCESS")
                    .verified(true)
                    .message("Payment verified successfully")
                    .build();

        } catch (Exception e) {
            log.error("Payment signature verification failed", e);
            return PaymentVerificationResponse.builder()
                    .status("ERROR")
                    .verified(false)
                    .message("Verification exception: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Handles asynchronous webhook events from Razorpay without needing any custom util classes.
     */
    @Transactional
    public void handleWebhookEvent(String rawPayload, String signature) {
        try {
            // Built-in Razorpay HMAC-SHA256 signature verification
            boolean isValid = Utils.verifyWebhookSignature(rawPayload, signature, webhookSecret);
            if (!isValid) {
                log.warn("Webhook signature mismatch");
                throw new IllegalArgumentException("Invalid Razorpay webhook signature");
            }
        } catch (Exception e) {
            log.error("Failed to verify webhook signature: {}", e.getMessage());
            throw new IllegalArgumentException("Signature verification failed", e);
        }

        JSONObject event = new JSONObject(rawPayload);
        String eventType = event.getString("event");

        JSONObject payloadObj = event.getJSONObject("payload");
        JSONObject paymentEntity = payloadObj.getJSONObject("payment").getJSONObject("entity");

        String razorpayOrderId = paymentEntity.optString("order_id", null);
        String razorpayPaymentId = paymentEntity.optString("id", null);

        if (razorpayOrderId == null) {
            log.info("Ignoring webhook event {}: No order_id present", eventType);
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("No payment found for order ID from webhook: {}", razorpayOrderId);
            return;
        }

        if ("payment.captured".equals(eventType)) {
            if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
                payment.setStatus("PAID");
                payment.setRazorpayPaymentId(razorpayPaymentId);
                paymentRepository.save(payment);

                updateRegistrationConfirmed(payment.getRegistrationId());
                log.info("Webhook updated payment to PAID for order: {}", razorpayOrderId);
            }
        } else if ("payment.failed".equals(eventType)) {
            if (!"PAID".equalsIgnoreCase(payment.getStatus())) {
                payment.setStatus("FAILED");
                payment.setRazorpayPaymentId(razorpayPaymentId);
                paymentRepository.save(payment);
                log.warn("Webhook marked payment as FAILED for order: {}", razorpayOrderId);
            }
        }
    }

    private void updateRegistrationConfirmed(String registrationId) {
        registrationRepository.findById(registrationId).ifPresent(reg -> {
            try {
                reg.setStatus(RegistrationStatus.valueOf("CONFIRMED"));
            } catch (IllegalArgumentException e) {
                reg.setStatus(RegistrationStatus.values()[RegistrationStatus.values().length - 1]);
            }
            registrationRepository.save(reg);
        });
    }
}