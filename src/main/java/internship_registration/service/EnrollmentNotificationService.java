package internship_registration.service;

import internship_registration.dto.StudentGroupResponse;
import internship_registration.entity.College;
import internship_registration.entity.InternshipBatch;
import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.Student;
import internship_registration.entity.User;
import internship_registration.entity.WhatsappGroup;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.repository.CollegeRepository;
import internship_registration.repository.InternshipBatchRepository;
import internship_registration.repository.InternshipRegistrationRepository;
import internship_registration.repository.WhatsappGroupRepository;
import internship_registration.security.AccessUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/** Success email after payment, with the WhatsApp group link of the student's college and batch. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentNotificationService {

    // Registrations that have paid
    private static final Set<RegistrationStatus> PAID_STATUSES = EnumSet.of(
            RegistrationStatus.ENROLLED, RegistrationStatus.IN_PROGRESS, RegistrationStatus.COMPLETED);

    private final WhatsappGroupRepository groupRepository;
    private final InternshipRegistrationRepository registrationRepository;
    private final InternshipBatchRepository batchRepository;
    private final CollegeRepository collegeRepository;
    private final EmailService emailService;

    // Optional: an admin address that is told when a batch has paid students but no WhatsApp group yet
    @Value("${app.admin.notify-email:}")
    private String adminNotifyEmail;

    /**
     * Called right after the payment is committed. It runs in its own transaction, so whatever happens here
     * can never undo the payment.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyEnrolled(String registrationId) {
        InternshipRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || reg.getStudent() == null || reg.getStudent().getUser() == null) {
            return;
        }
        Student student = reg.getStudent();
        User user = student.getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        InternshipBatch batch = batchRepository.findById(reg.getBatchId()).orElse(null);
        String batchName = batch != null ? batch.getBatchName() : "your batch";
        String courseName = batch != null && batch.getCourse() != null ? batch.getCourse().getName() : "the internship";

        Optional<WhatsappGroup> group = findActiveGroup(student.getCollegeId(), reg.getBatchId());

        if (group.isPresent()) {
            reg.setWhatsappLinkSentAt(LocalDateTime.now());
            registrationRepository.save(reg);
            emailService.sendEnrollmentEmail(user.getEmail(), user.getName(), courseName, batchName,
                    group.get().getGroupName(), group.get().getGroupUrl());
        } else {
            emailService.sendEnrollmentEmail(user.getEmail(), user.getName(), courseName, batchName, null, null);
            notifyAdminGroupNeeded(student.getCollegeId(), reg.getBatchId(), batchName);
        }
    }

    /**
     * Admin: emails the group link to paid students of this group's college and batch who have not received it yet
     * (or to all of them when resend is true). Returns how many students were emailed.
     */
    @Transactional
    public int sendPendingLinks(Integer groupId, boolean resend) {
        WhatsappGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("WhatsApp group not found: " + groupId));
        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new IllegalArgumentException("This WhatsApp group is inactive");
        }

        InternshipBatch batch = batchRepository.findById(group.getBatchId()).orElse(null);
        String batchName = batch != null ? batch.getBatchName() : "your batch";
        String courseName = batch != null && batch.getCourse() != null ? batch.getCourse().getName() : "the internship";

        List<InternshipRegistration> registrations = registrationRepository
                .findPaidByBatchAndCollege(group.getBatchId(), group.getCollegeId(), PAID_STATUSES);

        int sent = 0;
        for (InternshipRegistration reg : registrations) {
            if (!resend && reg.getWhatsappLinkSentAt() != null) {
                continue;
            }
            User user = reg.getStudent() != null ? reg.getStudent().getUser() : null;
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            emailService.sendEnrollmentEmail(user.getEmail(), user.getName(), courseName, batchName,
                    group.getGroupName(), group.getGroupUrl());
            reg.setWhatsappLinkSentAt(LocalDateTime.now());
            registrationRepository.save(reg);
            sent++;
        }
        return sent;
    }

    /** The group link of a paid registration (student, the cyber cafe that registered them, or an admin). */
    @Transactional(readOnly = true)
    public StudentGroupResponse getGroupForRegistration(String registrationId, String callerUserId,
                                                        boolean callerIsAdmin) {
        InternshipRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found: " + registrationId));
        if (!callerIsAdmin && !AccessUtils.canAccess(reg, callerUserId)) {
            throw new AccessDeniedException("You do not have access to this registration");
        }
        if (reg.getStatus() == null || !PAID_STATUSES.contains(reg.getStatus())) {
            throw new IllegalArgumentException("The WhatsApp group link is available after the payment is confirmed");
        }
        WhatsappGroup group = findActiveGroup(reg.getStudent().getCollegeId(), reg.getBatchId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Your WhatsApp group is not ready yet. The link will be emailed to you."));
        return new StudentGroupResponse(group.getGroupName(), group.getGroupUrl());
    }

    // ------------------------------------------------------------ helpers
    private Optional<WhatsappGroup> findActiveGroup(Integer collegeId, Integer batchId) {
        return groupRepository.findByCollegeIdAndBatchId(collegeId, batchId)
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()));
    }

    // Tell the admin once per batch: only when this is the first paid student still waiting for a link
    private void notifyAdminGroupNeeded(Integer collegeId, Integer batchId, String batchName) {
        if (adminNotifyEmail == null || adminNotifyEmail.isBlank()) {
            return;
        }
        List<InternshipRegistration> paid = registrationRepository
                .findPaidByBatchAndCollege(batchId, collegeId, PAID_STATUSES);
        long waiting = paid.stream().filter(r -> r.getWhatsappLinkSentAt() == null).count();
        if (waiting != 1) {
            return;
        }
        String collegeName = collegeRepository.findById(collegeId).map(College::getName)
                .orElse("college " + collegeId);
        emailService.sendNotice(adminNotifyEmail.trim(), "WhatsApp group needed: " + collegeName + " / " + batchName,
                "A student of " + collegeName + " has paid for batch " + batchName
                        + ", but no WhatsApp group exists for them yet.\n\n"
                        + "Create the group in WhatsApp, save its invite link in the admin panel "
                        + "(POST /api/admin/whatsapp-groups), then call "
                        + "POST /api/admin/whatsapp-groups/{id}/send-pending to email the link to the waiting students.");
    }
}