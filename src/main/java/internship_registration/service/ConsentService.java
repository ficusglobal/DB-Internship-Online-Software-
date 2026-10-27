package internship_registration.service;

import internship_registration.dto.ConsentSummaryResponse;
import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.User;
import internship_registration.entity.enums.ConsentStatus;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.exception.ResourceInUseException;
import internship_registration.repository.InternshipRegistrationRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/** Consent letter: the student uploads it before paying, an admin approves or rejects it. */
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final InternshipRegistrationRepository registrationRepository;
    private final FileStorageService fileStorageService;

    /** A stored consent file ready to be sent to the browser. */
    public record ConsentFile(Resource resource, String contentType, String fileName) {
    }

    // ------------------------------------------------------------- upload
    @Transactional
    public ConsentSummaryResponse upload(String registrationId, MultipartFile file,
                                         String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = loadWithAccess(registrationId, callerUserId, callerIsAdmin);

        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            throw new IllegalArgumentException("This registration is cancelled");
        }
        if (reg.getConsentStatus() == ConsentStatus.APPROVED && !callerIsAdmin) {
            throw new ResourceInUseException("The consent letter is already approved and cannot be replaced");
        }

        String oldPath = reg.getConsentLetterUrl();
        String newPath = fileStorageService.storeConsent(file, registrationId);

        reg.setConsentLetterUrl(newPath);
        reg.setConsentStatus(ConsentStatus.PENDING);   // a new file always needs a fresh review
        reg.setConsentRejectionReason(null);           // the old rejection no longer applies
        reg.setConsentReviewedAt(null);
        registrationRepository.save(reg);

        // Remove the replaced file only after the database change is safely committed
        if (oldPath != null && !oldPath.equals(newPath)) {
            runAfterCommit(() -> fileStorageService.deleteQuietly(oldPath));
        }
        return toSummary(reg);
    }

    // ----------------------------------------------------------- download
    @Transactional(readOnly = true)
    public ConsentFile download(String registrationId, String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = loadWithAccess(registrationId, callerUserId, callerIsAdmin);
        String path = reg.getConsentLetterUrl();
        if (path == null || path.isBlank()) {
            throw new NoSuchElementException("No consent letter has been uploaded for this registration");
        }
        return new ConsentFile(fileStorageService.load(path),
                fileStorageService.contentTypeOf(path),
                fileStorageService.fileNameOf(path));
    }

    // -------------------------------------------------------------- admin
    @Transactional(readOnly = true)
    public List<ConsentSummaryResponse> list(ConsentStatus status) {
        List<InternshipRegistration> registrations = status != null
                ? registrationRepository.findByConsentStatusAndConsentLetterUrlIsNotNullOrderByCreatedAtDesc(status)
                : registrationRepository.findByConsentLetterUrlIsNotNullOrderByCreatedAtDesc();
        List<ConsentSummaryResponse> result = new ArrayList<>();
        for (InternshipRegistration reg : registrations) {
            result.add(toSummary(reg));
        }
        return result;
    }

    // Student (or the cyber cafe, or an admin): where does the consent letter stand?
    @Transactional(readOnly = true)
    public ConsentSummaryResponse status(String registrationId, String callerUserId, boolean callerIsAdmin) {
        return toSummary(loadWithAccess(registrationId, callerUserId, callerIsAdmin));
    }

    @Transactional
    public ConsentSummaryResponse review(String registrationId, ConsentStatus status, String reason) {
        if (status == null || status == ConsentStatus.PENDING) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }
        String cleanReason = reason == null ? null : reason.trim();
        if (status == ConsentStatus.REJECTED) {
            if (cleanReason == null || cleanReason.isEmpty()) {
                throw new IllegalArgumentException("Please give a reason, so the student knows what to correct");
            }
            if (cleanReason.length() > 500) {
                throw new IllegalArgumentException("The reason can be at most 500 characters");
            }
        }
        InternshipRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found: " + registrationId));
        if (reg.getConsentLetterUrl() == null || reg.getConsentLetterUrl().isBlank()) {
            throw new IllegalArgumentException("No consent letter has been uploaded for this registration yet");
        }
        reg.setConsentStatus(status);
        reg.setConsentRejectionReason(status == ConsentStatus.REJECTED ? cleanReason : null);
        reg.setConsentReviewedAt(LocalDateTime.now());
        registrationRepository.save(reg);
        return toSummary(reg);
    }

    // ------------------------------------------------------------ helpers
    private InternshipRegistration loadWithAccess(String registrationId, String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found: " + registrationId));
        if (!callerIsAdmin && !AccessUtils.canAccess(reg, callerUserId)) {
            throw new AccessDeniedException("You do not have access to this registration");
        }
        return reg;
    }

    private ConsentSummaryResponse toSummary(InternshipRegistration reg) {
        User user = reg.getStudent() != null ? reg.getStudent().getUser() : null;
        return ConsentSummaryResponse.builder()
                .registrationId(reg.getId())
                .studentName(user != null ? user.getName() : null)
                .email(user != null ? user.getEmail() : null)
                .mobileNo(user != null ? user.getMobileNo() : null)
                .collegeId(reg.getStudent() != null ? reg.getStudent().getCollegeId() : null)
                .batchId(reg.getBatchId())
                .registrationStatus(reg.getStatus())
                .consentStatus(reg.getConsentStatus())
                .consentUploaded(reg.getConsentLetterUrl() != null && !reg.getConsentLetterUrl().isBlank())
                .rejectionReason(reg.getConsentRejectionReason())
                .reviewedAt(reg.getConsentReviewedAt())
                .build();
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}