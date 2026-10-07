package internship_registration.service;

import internship_registration.entity.enums.OtpPurpose;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name:Ficus Internship Portal}")
    private String fromName;

    /**
     * Runs on a background thread. Failures are logged, never thrown, so the API response
     * is identical whether or not the SMTP server is reachable. The OTP is never logged.
     */
    @Async
    public void sendOtpEmail(String to, String name, String otp, OtpPurpose purpose, int expiryMinutes) {
        try {
            String subject;
            String intro;
            if (purpose == OtpPurpose.PASSWORD_RESET) {
                subject = "Your password reset code";
                intro = "We received a request to reset your password. Use this code to continue:";
            } else {
                subject = "Verify your email address";
                intro = "Use this code to verify your email address:";
            }

            String displayName = (name == null || name.isBlank()) ? "there" : name;

            String plain = "Hello " + displayName + ",\n\n"
                    + intro + "\n\n"
                    + otp + "\n\n"
                    + "This code expires in " + expiryMinutes + " minutes and can be used only once.\n"
                    + "If you did not request this, you can safely ignore this email.\n\n"
                    + fromName;

            String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;\">"
                    + "<p>Hello " + HtmlUtils.htmlEscape(displayName) + ",</p>"
                    + "<p>" + intro + "</p>"
                    + "<p style=\"font-size:32px;letter-spacing:8px;font-weight:bold;"
                    + "background:#f3f4f6;padding:16px;text-align:center;border-radius:8px;\">" + otp + "</p>"
                    + "<p>This code expires in <b>" + expiryMinutes + " minutes</b> and can be used only once.</p>"
                    + "<p style=\"color:#6b7280;font-size:13px;\">If you did not request this, "
                    + "you can safely ignore this email.</p>"
                    + "<p>" + HtmlUtils.htmlEscape(fromName) + "</p>"
                    + "</div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plain, html);
            mailSender.send(message);

            log.info("{} OTP email sent to {}", purpose, mask(to));
        } catch (Exception e) {
            log.error("Failed to send {} OTP email to {}: {}", purpose, mask(to), e.getMessage());
        }
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.charAt(0) + "***" + email.substring(at);
    }
}
