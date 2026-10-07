package internship_registration.service;

import internship_registration.dto.AcceptanceLetterResponse;
import internship_registration.entity.College;
import internship_registration.entity.InternshipBatch;
import internship_registration.entity.InternshipCourse;
import internship_registration.entity.InternshipRegistration;
import internship_registration.entity.Student;
import internship_registration.entity.StudentAcceptanceLetter;
import internship_registration.entity.University;
import internship_registration.entity.User;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.repository.CollegeRepository;
import internship_registration.repository.InternshipBatchRepository;
import internship_registration.repository.InternshipRegistrationRepository;
import internship_registration.repository.StudentAcceptanceLetterRepository;
import internship_registration.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcceptanceLetterService {

    // Letter number looks like: FG/<COLLEGE_CODE>/<SESSION>/<SERIAL>   e.g. FG/GPM/2024-2027/1
    // The serial starts at 1 for every college (and session) and goes up by one for each new letter.
    private static final String LETTER_PREFIX = "FG";
    // 0 = no zero padding (1, 2, 3 ...). Set to 3 for 001, 002 ... or 4 for 0001, 0002 ...
    private static final int SERIAL_PADDING = 0;
    private static final int MAX_COLLEGE_CODE_LENGTH = 30;   // column is VARCHAR(30)
    private static final int MAX_SESSION_LENGTH = 20;        // column is VARCHAR(20)
    private static final int MAX_ALLOCATION_ATTEMPTS = 5;

    // Letter is only issued once payment is confirmed
    private static final Set<RegistrationStatus> ELIGIBLE_STATUSES =
            EnumSet.of(RegistrationStatus.ENROLLED, RegistrationStatus.IN_PROGRESS, RegistrationStatus.COMPLETED);

    private final StudentAcceptanceLetterRepository letterRepository;
    private final InternshipRegistrationRepository registrationRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipBatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final PlatformTransactionManager transactionManager;

    /** Idempotent: returns the existing letter if one was already generated. */
    public AcceptanceLetterResponse generate(String registrationId, String callerUserId, boolean callerIsAdmin) {
        InternshipRegistration registration = loadAuthorizedRegistration(registrationId, callerUserId, callerIsAdmin);

        Optional<StudentAcceptanceLetter> existing = letterRepository.findByRegistrationId(registrationId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        if (!ELIGIBLE_STATUSES.contains(registration.getStatus())) {
            throw new IllegalArgumentException(
                    "Acceptance letter can be generated only after payment is confirmed (current status: "
                            + registration.getStatus() + ")");
        }

        Student student = registration.getStudent();
        College college = collegeRepository.findById(student.getCollegeId())
                .orElseThrow(() -> new NoSuchElementException("College not found for this student"));

        String collegeCode = college.getCode();
        String session = student.getAcademicSession();
        if (collegeCode == null || collegeCode.isBlank()) {
            throw new IllegalArgumentException("College code is not configured for " + college.getName());
        }
        collegeCode = collegeCode.trim();
        session = session.trim();
        if (collegeCode.length() > MAX_COLLEGE_CODE_LENGTH) {
            throw new IllegalArgumentException("College code is longer than " + MAX_COLLEGE_CODE_LENGTH + " characters");
        }
        if (session.length() > MAX_SESSION_LENGTH) {
            throw new IllegalArgumentException("Academic session is longer than " + MAX_SESSION_LENGTH + " characters");
        }

        final String code = collegeCode;
        final String acadSession = session;
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Two requests can pick the same next number; the unique key rejects one, and we retry.
        for (int attempt = 1; attempt <= MAX_ALLOCATION_ATTEMPTS; attempt++) {
            try {
                StudentAcceptanceLetter saved = tx.execute(status -> allocate(registrationId, code, acadSession));
                return toResponse(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("Letter number collision for {}/{} (attempt {})", code, acadSession, attempt);
                Optional<StudentAcceptanceLetter> created = letterRepository.findByRegistrationId(registrationId);
                if (created.isPresent()) {
                    return toResponse(created.get()); // a parallel request already made this letter
                }
            }
        }
        throw new IllegalStateException("Could not allocate an acceptance letter number. Please retry.");
    }

    public AcceptanceLetterResponse getByRegistration(String registrationId, String callerUserId, boolean callerIsAdmin) {
        loadAuthorizedRegistration(registrationId, callerUserId, callerIsAdmin);
        return letterRepository.findByRegistrationId(registrationId)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Acceptance letter has not been generated yet"));
    }

    /** Letter for the logged-in student's latest registration. */
    public AcceptanceLetterResponse getMine(String callerUserId) {
        Student student = studentRepository.findByUser_Id(callerUserId)
                .orElseThrow(() -> new NoSuchElementException("Student profile not found"));
        InternshipRegistration registration = registrationRepository
                .findFirstByStudent_IdOrderByCreatedAtDesc(student.getId())
                .orElseThrow(() -> new NoSuchElementException("No internship registration found"));
        return letterRepository.findByRegistrationId(registration.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Acceptance letter has not been generated yet"));
    }

    public List<AcceptanceLetterResponse> search(String collegeCode, String academicSession) {
        String code = (collegeCode == null || collegeCode.isBlank()) ? null : collegeCode.trim();
        String session = (academicSession == null || academicSession.isBlank()) ? null : academicSession.trim();
        return letterRepository.search(code, session).stream().map(this::toResponse).toList();
    }

    // ------------------------------------------------------------------ INTERNALS

    private StudentAcceptanceLetter allocate(String registrationId, String collegeCode, String session) {
        Optional<StudentAcceptanceLetter> existing = letterRepository.findByRegistrationId(registrationId);
        if (existing.isPresent()) {
            return existing.get();
        }
        int next = letterRepository.findMaxSequence(collegeCode, session) + 1;
        String serial = SERIAL_PADDING > 0
                ? String.format("%0" + SERIAL_PADDING + "d", next)
                : String.valueOf(next);
        String letterNumber = LETTER_PREFIX + "/" + collegeCode + "/" + session + "/" + serial;

        return letterRepository.save(StudentAcceptanceLetter.builder()
                .registrationId(registrationId)
                .collegeCode(collegeCode)
                .academicSession(session)
                .sequenceNumber(next)
                .letterNumber(letterNumber)
                .build());
    }

    /** Owner student, the cyber cafe that registered them, or an admin. */
    private InternshipRegistration loadAuthorizedRegistration(String registrationId, String callerUserId, boolean admin) {
        InternshipRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found: " + registrationId));

        if (admin) {
            return registration;
        }
        boolean isOwner = registration.getStudent() != null
                && registration.getStudent().getUser() != null
                && callerUserId.equals(registration.getStudent().getUser().getId());
        boolean isRegisteringCafe = registration.getCyberCafe() != null
                && registration.getCyberCafe().getUser() != null
                && callerUserId.equals(registration.getCyberCafe().getUser().getId());

        if (!isOwner && !isRegisteringCafe) {
            throw new AccessDeniedException("You do not have access to this registration");
        }
        return registration;
    }

    /** Builds the full letter data: letter + student + college + university + batch + course. */
    private AcceptanceLetterResponse toResponse(StudentAcceptanceLetter l) {
        InternshipRegistration reg = registrationRepository.findById(l.getRegistrationId()).orElse(null);
        Student student = reg != null ? reg.getStudent() : null;
        User user = student != null ? student.getUser() : null;

        College college = (student != null && student.getCollegeId() != null)
                ? collegeRepository.findById(student.getCollegeId()).orElse(null) : null;
        University university = college != null ? college.getUniversity() : null;

        InternshipBatch batch = (reg != null && reg.getBatchId() != null)
                ? batchRepository.findById(reg.getBatchId()).orElse(null) : null;
        InternshipCourse course = batch != null ? batch.getCourse() : null;

        return AcceptanceLetterResponse.builder()
                // letter
                .id(l.getId())
                .letterNumber(l.getLetterNumber())
                .sequenceNumber(l.getSequenceNumber())
                .generatedAt(l.getGeneratedAt())
                // student
                .registrationId(l.getRegistrationId())
                .studentId(student != null ? student.getId() : null)
                .studentName(user != null ? user.getName() : null)
                .fatherName(student != null ? student.getFatherName() : null)
                .gender(student != null && student.getGender() != null ? student.getGender().name() : null)
                .email(user != null ? user.getEmail() : null)
                .mobileNo(user != null ? user.getMobileNo() : null)
                .studentRegistrationNumber(student != null ? student.getRegistrationNumber() : null)
                .collegeRollNumber(student != null ? student.getCollegeRollNumber() : null)
                .degree(student != null ? student.getDegree() : null)
                .department(student != null ? student.getDepartment() : null)
                .majorSubject(student != null ? student.getMajorSubject() : null)
                .academicSession(l.getAcademicSession())
                // college / university
                .collegeCode(l.getCollegeCode())
                .collegeName(college != null ? college.getName() : null)
                .collegeAddress(college != null
                        ? joinNonBlank(college.getAddressLine1(), college.getCity(), college.getState(), college.getPincode())
                        : null)
                .city(college != null ? college.getCity() : null)
                .state(college != null ? college.getState() : null)
                .pincode(college != null ? college.getPincode() : null)
                .district(college != null && college.getDistrict() != null ? college.getDistrict().getName() : null)
                .universityName(university != null ? university.getName() : null)
                .universityCode(university != null ? university.getCode() : null)
                // internship
                .internshipRegistrationNumber(reg != null ? reg.getRegistrationNumber() : null)
                .registrationStatus(reg != null && reg.getStatus() != null ? reg.getStatus().name() : null)
                .batchName(batch != null ? batch.getBatchName() : null)
                .batchStartDate(batch != null ? batch.getStartDate() : null)
                .batchEndDate(batch != null ? batch.getEndDate() : null)
                .courseName(course != null ? course.getName() : null)
                .courseCode(course != null ? course.getCode() : null)
                .theoryDurationHours(course != null ? course.getTheoryDurationHours() : null)
                .practicalDurationHours(course != null ? course.getPracticalDurationHours() : null)
                .reportPreparationHours(course != null ? course.getReportPreparationHours() : null)
                .totalDurationHours(course != null ? course.getTotalDurationHours() : null)
                .build();
    }

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(part.trim());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}