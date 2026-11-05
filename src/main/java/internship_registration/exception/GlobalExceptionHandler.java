package internship_registration.exception;

import com.razorpay.RazorpayException;
import internship_registration.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Turns every exception into the same JSON shape:
 * { "timestamp", "status", "error", "message", "validationErrors" (only for field errors) }
 *
 * Spring picks the MOST SPECIFIC handler, so e.g. DuplicateResourceException uses its own handler
 * even though it is also a RuntimeException.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Same setting as the upload limit, so the message always shows the real limit
    @Value("${app.upload.max-size-mb:1}")
    private long maxUploadMb;

    // =====================================================================
    // APPLICATION EXCEPTIONS
    // =====================================================================

    // 0. OTP failures: invalid/expired code (400), too many attempts or resend too soon (429)
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponse> handleOtpException(OtpException ex) {
        return build(ex.getStatus(), "OTP Verification Failed", ex.getMessage());
    }

    // 0b. Duplicate data: email / mobile / registration no. / roll no. / master data that already exists (409)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, "Duplicate Entry", ex.getMessage());
    }

    // 0c. Record still used by other records, e.g. deleting a session that students use (409)
    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ErrorResponse> handleResourceInUse(ResourceInUseException ex) {
        return build(HttpStatus.CONFLICT, "Resource In Use", ex.getMessage());
    }

    // =====================================================================
    // REQUEST PROBLEMS (400 / 404 / 405 / 406 / 413 / 415)
    // =====================================================================

    // 1. @Valid field validation failures (400) -> validationErrors = { field: message }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage()); // first message per field
        }
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(errors.size() == 1 ? errors.values().iterator().next()
                        : "One or more input fields are invalid")
                .validationErrors(errors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Missing or malformed JSON body, or a wrong type inside it, e.g. "semester": "five" (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request received: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed Request",
                "Request body is missing, is not valid JSON, or a field has the wrong type");
    }

    // 3. Missing query parameter, e.g. ?degree= missing (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Missing Query Parameter",
                "Required query parameter '" + ex.getParameterName() + "' is missing");
    }

    // 4. Wrong type in the URL, e.g. a word where an Integer id is expected (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String type = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return build(HttpStatus.BAD_REQUEST, "Type Mismatch",
                String.format("Parameter '%s' should be of type '%s'", ex.getName(), type));
    }

    // 5. Missing required header, e.g. X-Razorpay-Signature on the webhook (400)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, "Missing Header",
                "Required header '" + ex.getHeaderName() + "' is not present");
    }

    // 6. The file part is missing from a multipart request (400)
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return build(HttpStatus.BAD_REQUEST, "Missing File",
                "Attach the file as the form field '" + ex.getRequestPartName() + "'");
    }

    // 7. Business rule failed, e.g. "Semester must be between 1 and 4 for PG" (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage());
    }

    // 8. Record not found by id (404)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Requested resource was not found");
    }

    // 9. Wrong URL, e.g. a typo or an extra "/" at the end (404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No endpoint found for {}", ex.getResourcePath());
        return build(HttpStatus.NOT_FOUND, "Not Found", "No endpoint exists at this URL: /" + ex.getResourcePath());
    }

    // 10. Wrong HTTP method, e.g. GET on a POST endpoint (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.getMessage());
    }

    // 11. Client asked for a format we can't produce (406)
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("Not acceptable: {}", ex.getMessage());
        return build(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", "This endpoint only returns JSON");
    }

    // 12. Uploaded file bigger than app.upload.max-size-mb (413)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
                "The file is too large. Maximum size is " + maxUploadMb + " MB.");
    }

    // 13. Wrong Content-Type, e.g. text/plain instead of JSON (415)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "Send the request body as JSON with the header Content-Type: application/json");
    }

    // =====================================================================
    // SECURITY (401 / 403)
    // =====================================================================

    // 14. Expired / tampered JWT (401)
    @ExceptionHandler({ExpiredJwtException.class, SignatureException.class, MalformedJwtException.class})
    public ResponseEntity<ErrorResponse> handleJwtExceptions(Exception ex) {
        log.warn("JWT validation issue: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid JWT Token", "Token has expired or is invalid. Please log in again.");
    }

    // 15. Wrong email/mobile or password (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email/mobile or password");
    }

    // 16. Disabled or locked account (403)
    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleDisabledAccount(RuntimeException ex) {
        return build(HttpStatus.FORBIDDEN, "Account Disabled", "Your account is disabled. Please contact support.");
    }

    // 17. Logged in but not allowed, e.g. a student calling an admin API (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                ex.getMessage() != null && !ex.getMessage().isBlank() && !"Access Denied".equals(ex.getMessage())
                        ? ex.getMessage()
                        : "You do not have permission to access this resource");
    }

    // =====================================================================
    // DATABASE (409 / 400)
    // =====================================================================

    // 18. Database rule broken: duplicate unique key (409), record still referenced (409), missing value (400)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Database constraint violation: {}", cause);
        String text = cause == null ? "" : cause.toLowerCase(Locale.ROOT);

        if (text.contains("duplicate")) {
            return build(HttpStatus.CONFLICT, "Duplicate Entry", duplicateMessage(text));
        }
        if (text.contains("foreign key")) {
            return build(HttpStatus.CONFLICT, "Related Record Problem",
                    "This record is linked to other records, or a linked record does not exist.");
        }
        if (text.contains("cannot be null") || text.contains("doesn't have a default value")) {
            return build(HttpStatus.BAD_REQUEST, "Missing Value", "A required value is missing.");
        }
        if (text.contains("data too long")) {
            return build(HttpStatus.BAD_REQUEST, "Value Too Long", "One of the values is longer than allowed.");
        }
        return build(HttpStatus.CONFLICT, "Conflict", "The data could not be saved because it breaks a database rule.");
    }

    // Friendly text for MySQL/TiDB "Duplicate entry '...' for key '...'" (matched on the column / key name)
    private String duplicateMessage(String text) {
        if (text.contains("email")) return "This email is already registered";
        if (text.contains("mobile")) return "This mobile number is already registered";
        if (text.contains("registration_number")) return "This registration number is already registered";
        if (text.contains("college_roll")) return "This roll number is already registered in this college";
        if (text.contains("razorpay_order") || text.contains("razorpay_payment")) return "This payment is already recorded";
        if (text.contains("letter") || text.contains("sequence_number"))
            return "Another acceptance letter was generated at the same moment. Please try again.";
        if (text.contains("academic_sessions")) return "This academic session already exists";
        if (text.contains("major_subjects")) return "This major subject already exists";
        if (text.contains("verification_token")) return "Please verify your email again";
        return "This record already exists";
    }

    // 19. Two requests changed the same row at the same time (TiDB lock conflict) (409)
    @ExceptionHandler({PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleLockConflict(RuntimeException ex) {
        log.warn("Lock conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Busy", "This record is being updated by another request. Please try again.");
    }

    // =====================================================================
    // PAYMENT GATEWAY (502)
    // =====================================================================

    // 20. Razorpay rejected or could not be reached while creating an order (502)
    @ExceptionHandler(RazorpayException.class)
    public ResponseEntity<ErrorResponse> handleRazorpay(RazorpayException ex) {
        log.error("Razorpay error: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Payment Gateway Error",
                "Could not reach the payment gateway. Please try again in a moment.");
    }

    // =====================================================================
    // FALLBACKS
    // =====================================================================

    // 21. Plain RuntimeException thrown by our services with a user-facing message, e.g. "Passwords do not match" (400).
    //     Any other runtime error (NullPointerException, IllegalStateException, ...) is a server bug: 500, details only in the log.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeExceptions(RuntimeException ex) {
        if (ex.getClass() == RuntimeException.class && ex.getMessage() != null) {
            log.warn("Business rule failed: {}", ex.getMessage());
            return build(HttpStatus.BAD_REQUEST, "Operation Failed", ex.getMessage());
        }
        log.error("Unexpected runtime error: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong on our side. Please try again later.");
    }

    // 22. Anything else (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalExceptions(Exception ex) {
        log.error("Critical server crash: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please contact system support.");
    }

    // ---------------------------------------------------------------- helper
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .build();
        return new ResponseEntity<>(response, status);
    }
}