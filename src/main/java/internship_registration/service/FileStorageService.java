package internship_registration.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Saves uploaded documents in the folder set by app.upload.dir (environment variable APP_UPLOAD_DIR).
 * Keep that folder OUTSIDE any web server folder (such as XAMPP htdocs); files are only served through the API.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final String CONSENT_DIR = "consents";

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size-mb:5}")
    private long maxSizeMb;

    private Path root;

    @PostConstruct
    void init() {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve(CONSENT_DIR));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create the upload folder " + root + ": " + e.getMessage(), e);
        }
        log.info("Uploaded files are stored in {}", root);
    }

    /** Validates and saves a consent letter. Returns the stored path relative to the upload folder. */
    public String storeConsent(MultipartFile file, String registrationId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please attach the consent letter");
        }
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new IllegalArgumentException("The file is too large. Maximum size is " + maxSizeMb + " MB");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the uploaded file", e);
        }

        // The type is decided by the file's content, never by its name or the browser's claim
        String extension = detectExtension(data);
        if (extension == null) {
            throw new IllegalArgumentException("Only PDF, JPG or PNG files are allowed");
        }

        // A generated name: the original name is never used
        String fileName = registrationId + "-" + UUID.randomUUID() + extension;
        Path target = root.resolve(CONSENT_DIR).resolve(fileName).normalize();
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded file", e);
        }
        return CONSENT_DIR + "/" + fileName;
    }

    public Resource load(String relativePath) {
        Path path;
        try {
            path = root.resolve(relativePath).normalize();
        } catch (InvalidPathException e) {
            throw new NoSuchElementException("The file could not be found on the server");
        }
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new NoSuchElementException("The file could not be found on the server");
        }
        return new FileSystemResource(path);
    }

    public String contentTypeOf(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }

    public String fileNameOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path path = root.resolve(relativePath).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            log.warn("Could not delete the old file {}: {}", relativePath, e.getMessage());
        }
    }

    private String detectExtension(byte[] d) {
        if (startsWith(d, 0x25, 0x50, 0x44, 0x46)) return ".pdf";                        // %PDF
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) return ".jpg";                              // JPEG
        if (startsWith(d, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return ".png"; // PNG
        return null;
    }

    private boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}