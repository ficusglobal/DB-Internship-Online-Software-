package internship_registration.controller;

import internship_registration.dto.MessageResponse;
import internship_registration.dto.StudentGroupResponse;
import internship_registration.security.AccessUtils;
import internship_registration.service.EnrollmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WhatsappLinkController {

    private final EnrollmentNotificationService notificationService;

    // Student: the group link, once the payment is confirmed
    @GetMapping("/student/registrations/{registrationId}/whatsapp-group")
    public ResponseEntity<StudentGroupResponse> getGroup(@PathVariable String registrationId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(notificationService.getGroupForRegistration(
                registrationId, authentication.getName(), AccessUtils.isAdmin(authentication)));
    }

    // Admin (SUPER_ADMIN via the /api/admin/** rule): email the link to paid students who are still waiting
    @PostMapping("/admin/whatsapp-groups/{id}/send-pending")
    public ResponseEntity<MessageResponse> sendPending(@PathVariable Integer id,
                                                       @RequestParam(defaultValue = "false") boolean resend) {
        int count = notificationService.sendPendingLinks(id, resend);
        return ResponseEntity.ok(new MessageResponse("Group link emailed to " + count + " student(s)"));
    }
}