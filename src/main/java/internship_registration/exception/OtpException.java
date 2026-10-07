package internship_registration.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OtpException extends RuntimeException {

    private final HttpStatus status;

    public OtpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // Same message for unknown email / wrong code / expired code so callers can't probe accounts
    public static OtpException invalid() {
        return new OtpException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
    }

    public static OtpException tooManyAttempts() {
        return new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many incorrect attempts. Please request a new OTP.");
    }
}
