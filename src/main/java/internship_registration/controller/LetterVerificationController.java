package internship_registration.controller;

import internship_registration.dto.LetterVerificationResponse;
import internship_registration.service.AcceptanceLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class LetterVerificationController {

    private final AcceptanceLetterService letterService;

    @GetMapping("/api/public/acceptance-letters/verify/{code}")
    public ResponseEntity<LetterVerificationResponse> verify(@PathVariable String code) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(letterService.verify(code));
    }
}