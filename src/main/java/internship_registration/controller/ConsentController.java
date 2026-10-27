package internship_registration.controller;

import internship_registration.dto.ConsentReviewRequest;
import internship_registration.dto.ConsentSummaryResponse;
import internship_registration.entity.enums.ConsentStatus;
import internship_registration.security.AccessUtils;
import internship_registration.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Consent letter endpoints.
 *  - /api/student/registrations/{id}/consent : the student (or the cyber cafe that registered them) uploads and views it
 *  - /api/admin/**                           : SUPER_ADMIN lists, approves or rejects (URL rule in SecurityConfig)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping(value = "/student/registrations/{registrationId}/consent",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConsentSummaryResponse> upload(@PathVariable String registrationId,
                                                         @RequestParam("file") MultipartFile file,
                                                         Authentication authentication) {
        return ResponseEntity.ok(consentService.upload(
                registrationId, file, authentication.getName(), AccessUtils.isAdmin(authentication)));
    }

    // Status for the student's page: PENDING / APPROVED / REJECTED, plus the rejection reason
    @GetMapping("/student/registrations/{registrationId}/consent/status")
    public ResponseEntity<ConsentSummaryResponse> status(@PathVariable String registrationId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(consentService.status(
                registrationId, authentication.getName(), AccessUtils.isAdmin(authentication)));
    }

    @GetMapping("/student/registrations/{registrationId}/consent")
    public ResponseEntity<Resource> download(@PathVariable String registrationId, Authentication authentication) {
        ConsentService.ConsentFile file = consentService.download(
                registrationId, authentication.getName(), AccessUtils.isAdmin(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    @GetMapping("/admin/consents")
    public ResponseEntity<List<ConsentSummaryResponse>> list(
            @RequestParam(required = false) ConsentStatus status) {
        return ResponseEntity.ok(consentService.list(status));
    }

    @PutMapping("/admin/registrations/{registrationId}/consent")
    public ResponseEntity<ConsentSummaryResponse> review(@PathVariable String registrationId,
                                                         @RequestBody ConsentReviewRequest request) {
        return ResponseEntity.ok(consentService.review(registrationId, request.getStatus(), request.getReason()));
    }
}