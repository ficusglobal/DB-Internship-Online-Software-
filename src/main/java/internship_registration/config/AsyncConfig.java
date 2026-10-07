package internship_registration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Lets EmailService send mail in the background so SMTP latency doesn't slow API responses
@Configuration
@EnableAsync
public class AsyncConfig {
}
