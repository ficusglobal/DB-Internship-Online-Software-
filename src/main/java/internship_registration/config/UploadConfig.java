package internship_registration.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/** Sets the upload limit to app.upload.max-size-mb (default 1 MB). */
@Configuration
public class UploadConfig {

    @Value("${app.upload.max-size-mb:1}")
    private long maxSizeMb;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(maxSizeMb));
        factory.setMaxRequestSize(DataSize.ofMegabytes(maxSizeMb + 1));
        return factory.createMultipartConfig();
    }
}