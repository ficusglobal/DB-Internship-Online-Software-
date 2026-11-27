package internship_registration.service;

import internship_registration.dto.StudentPhotoResponse;
import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.Student;
import internship_registration.entity.StudentAcceptanceLetter;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.exception.ResourceInUseException;
import internship_registration.repository.InternshipRegistrationRepository;
import internship_registration.repository.StudentAcceptanceLetterRepository;
import internship_registration.repository.StudentRepository;
import internship_registration.security.AccessUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class StudentPhotoService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-f0-9]{32}$");

    private final InternshipRegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final StudentAcceptanceLetterRepository letterRepository;
    private final FileStorageService fileStorageService;

    /** A stored photo ready to be sent to the browser. */
    public record PhotoFile(Resource resource, String contentType, String fileName) {
    }

    // ------------------------------------------------------------------ URLs used in responses

    /** Photo of a registration's student, for logged-in users (student, its cyber cafe, admin). */
    public static String photoPath(String registrationId) {
        return "/api/student/registrations/" + registrationId + "/photo";
    }

    /** Photo on the public QR check (no login), found by the letter's verification code. */
    public static String publicPhotoPath(String verificationCode) {
        return "/api/public/acceptance-letters/verify/" + verificationCode + "/photo";
    }

    // ------------------------------------------------------------------ upload / status / view

    @Transactional
    public StudentPhotoResponse upload(String registrationId, MultipartFile file,
                                       String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = loadWithAccess(registrationId, callerUserId, callerIsAdmin);
        if (reg.getStatus() == RegistrationStatus.CANCELLED && !callerIsAdmin) {
            throw new IllegalArgumentException("This registration is cancelled");
        }
        boolean letterIssued = letterRepository.findByRegistrationId(registrationId).isPresent();
        if (letterIssued && !callerIsAdmin) {
            throw new ResourceInUseException(
                    "The photo cannot be changed after the acceptance letter is issued. Please contact the admin.");
        }

        Student student = reg.getStudent();
        String oldPath = student.getPhotoUrl();
        String newPath = fileStorageService.storePhoto(file, student.getId());

        student.setPhotoUrl(newPath);
        student.setPhotoUploadedAt(LocalDateTime.now());
        studentRepository.save(student);

        // Remove the replaced file only after the database change is safely committed;
        // if the save fails, remove the new file instead
        runAfterCommit(() -> {
            if (oldPath != null && !oldPath.equals(newPath)) {
                fileStorageService.deleteQuietly(oldPath);
            }
        }, () -> fileStorageService.deleteQuietly(newPath));

        return toResponse(reg, student, letterIssued, callerIsAdmin);
    }

    @Transactional(readOnly = true)
    public StudentPhotoResponse status(String registrationId, String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = loadWithAccess(registrationId, callerUserId, callerIsAdmin);
        boolean letterIssued = letterRepository.findByRegistrationId(registrationId).isPresent();
        return toResponse(reg, reg.getStudent(), letterIssued, callerIsAdmin);
    }

    @Transactional(readOnly = true)
    public PhotoFile view(String registrationId, String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = loadWithAccess(registrationId, callerUserId, callerIsAdmin);
        return fileOf(reg.getStudent());
    }

    /** Public: photo of the student on the acceptance letter with this QR code. */
    @Transactional(readOnly = true)
    public PhotoFile viewByVerificationCode(String code) {
        String c = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(c).matches()) {
            throw new NoSuchElementException("This QR code is not valid");
        }
        StudentAcceptanceLetter letter = letterRepository.findByVerificationCode(c)
                .orElseThrow(() -> new NoSuchElementException("This QR code is not valid"));
        InternshipRegistration reg = registrationRepository.findById(letter.getRegistrationId())
                .orElseThrow(() -> new NoSuchElementException("This QR code is not valid"));
        return fileOf(reg.getStudent());
    }

    // ------------------------------------------------------------------ helpers

    private PhotoFile fileOf(Student student) {
        String path = student != null ? student.getPhotoUrl() : null;
        if (path == null || path.isBlank()) {
            throw new NoSuchElementException("No photo has been uploaded for this student");
        }
        return new PhotoFile(fileStorageService.load(path),
                fileStorageService.contentTypeOf(path),
                fileStorageService.fileNameOf(path));
    }

    private InternshipRegistration loadWithAccess(String registrationId, String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found: " + registrationId));
        if (!callerIsAdmin && !AccessUtils.canAccess(reg, callerUserId)) {
            throw new AccessDeniedException("You do not have access to this registration");
        }
        if (reg.getStudent() == null) {
            throw new NoSuchElementException("Student not found for registration: " + registrationId);
        }
        return reg;
    }

    private StudentPhotoResponse toResponse(InternshipRegistration reg, Student student,
                                            boolean letterIssued, boolean callerIsAdmin) {
        boolean hasPhoto = student.getPhotoUrl() != null && !student.getPhotoUrl().isBlank();
        return StudentPhotoResponse.builder()
                .registrationId(reg.getId())
                .studentId(student.getId())
                .hasPhoto(hasPhoto)
                .photoUrl(hasPhoto ? photoPath(reg.getId()) : null)
                .photoUploadedAt(student.getPhotoUploadedAt())
                .canChange(callerIsAdmin || !letterIssued)
                .build();
    }

    private static void runAfterCommit(Runnable onCommit, Runnable onRollback) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        onCommit.run();
                    } else {
                        onRollback.run();
                    }
                }
            });
        } else {
            onCommit.run();
        }
    }
}