package internship_registration.controller;

import internship_registration.dto.AcceptanceLetterResponse;
import internship_registration.service.AcceptanceLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcceptanceLetterController {

    private final AcceptanceLetterService letterService;

    // Student / cyber cafe / admin: generate (idempotent)
    @PostMapping("/acceptance-letters/registrations/{registrationId}")
    public ResponseEntity<AcceptanceLetterResponse> generate(
            @PathVariable String registrationId, Authentication authentication) {
        return ResponseEntity.ok(letterService.generate(
                registrationId, authentication.getName(), isAdmin(authentication)));
    }

    @GetMapping("/acceptance-letters/registrations/{registrationId}")
    public ResponseEntity<AcceptanceLetterResponse> getByRegistration(
            @PathVariable String registrationId, Authentication authentication) {
        return ResponseEntity.ok(letterService.getByRegistration(
                registrationId, authentication.getName(), isAdmin(authentication)));
    }

    // Logged-in student's own letter
    @GetMapping("/acceptance-letters/me")
    public ResponseEntity<AcceptanceLetterResponse> getMine(Authentication authentication) {
        return ResponseEntity.ok(letterService.getMine(authentication.getName()));
    }

    // SUPER_ADMIN only (enforced by the /api/admin/** rule in SecurityConfig)
    @GetMapping("/admin/acceptance-letters")
    public ResponseEntity<List<AcceptanceLetterResponse>> search(
            @RequestParam(required = false) String collegeCode,
            @RequestParam(required = false) String academicSession) {
        return ResponseEntity.ok(letterService.search(collegeCode, academicSession));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("SUPER_ADMIN") || a.equals("UNIVERSITY_ADMIN"));
    }
}
