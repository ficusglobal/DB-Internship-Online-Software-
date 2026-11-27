package internship_registration.controller;

import internship_registration.dto.StudentPhotoResponse;
import internship_registration.security.AccessUtils;
import internship_registration.service.StudentPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;


@RestController
@RequiredArgsConstructor
public class StudentPhotoController {

    private final StudentPhotoService photoService;

    @PostMapping(value = "/api/student/registrations/{registrationId}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentPhotoResponse> upload(@PathVariable String registrationId,
                                                       @RequestParam("file") MultipartFile file,
                                                       Authentication authentication) {
        return ResponseEntity.ok(photoService.upload(
                registrationId, file, authentication.getName(), AccessUtils.isAdmin(authentication)));
    }

    @GetMapping("/api/student/registrations/{registrationId}/photo/status")
    public ResponseEntity<StudentPhotoResponse> status(@PathVariable String registrationId,
                                                       Authentication authentication) {
        return ResponseEntity.ok(photoService.status(
                registrationId, authentication.getName(), AccessUtils.isAdmin(authentication)));
    }

    @GetMapping("/api/student/registrations/{registrationId}/photo")
    public ResponseEntity<Resource> view(@PathVariable String registrationId, Authentication authentication) {
        StudentPhotoService.PhotoFile photo = photoService.view(
                registrationId, authentication.getName(), AccessUtils.isAdmin(authentication));
        return image(photo, "private, max-age=300");
    }

    @GetMapping("/api/public/acceptance-letters/verify/{code}/photo")
    public ResponseEntity<Resource> publicView(@PathVariable String code) {
        StudentPhotoService.PhotoFile photo = photoService.viewByVerificationCode(code);
        return image(photo, "no-store");
    }

    private static ResponseEntity<Resource> image(StudentPhotoService.PhotoFile photo, String cacheControl) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("photo" + extensionOf(photo.fileName())).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(photo.resource());
    }

    private static String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : ".jpg";
    }
}