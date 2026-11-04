package internship_registration.service;

import internship_registration.dto.AcademicSessionRequest;
import internship_registration.dto.MajorSubjectRequest;
import internship_registration.dto.OptionResponse;
import internship_registration.entity.AcademicSession;
import internship_registration.entity.MajorSubject;
import internship_registration.entity.enums.DegreeLevel;
import internship_registration.entity.enums.Department;
import internship_registration.exception.DuplicateResourceException;
import internship_registration.exception.ResourceInUseException;
import internship_registration.repository.AcademicSessionRepository;
import internship_registration.repository.MajorSubjectRepository;
import internship_registration.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registration dropdowns: degree (UG/PG), department, semester, academic session, major subject.
 * Degree, department and semester are fixed lists (enums); sessions and subjects are tables with admin CRUD.
 */
@Service
@RequiredArgsConstructor
public class AcademicMasterService {

    private static final Pattern SESSION_PATTERN = Pattern.compile("^(\\d{4})\\s*-\\s*(\\d{4})$");
    private static final int MAX_SESSION_YEARS = 6;

    private final AcademicSessionRepository sessionRepository;
    private final MajorSubjectRepository subjectRepository;
    private final StudentRepository studentRepository;

    // =====================================================================
    // FIXED DROPDOWNS
    // =====================================================================
    public List<OptionResponse> getDegrees() {
        List<OptionResponse> result = new ArrayList<>();
        for (DegreeLevel d : DegreeLevel.values()) {
            result.add(OptionResponse.builder()
                    .code(d.name()).label(d.getLabel()).maxSemester(d.getMaxSemester()).build());
        }
        return result;
    }

    /** degree = UG / PG, or null for all departments. */
    public List<OptionResponse> getDepartments(String degree) {
        DegreeLevel level = (degree == null || degree.isBlank()) ? null : DegreeLevel.parse(degree);
        List<OptionResponse> result = new ArrayList<>();
        for (Department d : Department.forDegree(level)) {
            result.add(OptionResponse.builder()
                    .code(d.name()).label(d.getLabel()).degree(d.getDegree().name()).build());
        }
        return result;
    }

    /** UG = 1..8, PG = 1..4 */
    public List<Integer> getSemesters(String degree) {
        DegreeLevel level = DegreeLevel.parse(degree);
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= level.getMaxSemester(); i++) {
            result.add(i);
        }
        return result;
    }

    // =====================================================================
    // ACADEMIC SESSION
    // =====================================================================
    public List<AcademicSession> getActiveSessions() {
        return sessionRepository.findByIsActiveTrueOrderByNameDesc();
    }

    public List<AcademicSession> getAllSessions() {
        return sessionRepository.findAllByOrderByNameDesc();
    }

    public AcademicSession getSession(Integer id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Academic session not found: " + id));
    }

    @Transactional
    public AcademicSession createSession(AcademicSessionRequest request) {
        String name = parseSession(request.getName());
        if (sessionRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Academic session already exists: " + name);
        }
        AcademicSession session = AcademicSession.builder()
                .name(name)
                .isActive(request.getActive() == null || request.getActive())
                .build();
        return sessionRepository.save(session);
    }

    @Transactional
    public AcademicSession updateSession(Integer id, AcademicSessionRequest request) {
        AcademicSession session = getSession(id);

        if (request.getName() != null) {
            String name = parseSession(request.getName());
            if (!name.equals(session.getName())) {
                if (sessionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                    throw new DuplicateResourceException("Academic session already exists: " + name);
                }
                long used = studentRepository.countUsingAcademicSession(session.getId(), session.getName());
                if (used > 0) {
                    throw new ResourceInUseException("Cannot rename this academic session because "
                            + used + " student(s) use it. Create a new session and deactivate this one instead.");
                }
                session.setName(name);
            }
        }
        if (request.getActive() != null) {
            session.setIsActive(request.getActive());
        }
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Integer id) {
        AcademicSession session = getSession(id);
        long used = studentRepository.countUsingAcademicSession(session.getId(), session.getName());
        if (used > 0) {
            throw new ResourceInUseException("Cannot delete this academic session because it is used by "
                    + used + " student(s). Deactivate it instead.");
        }
        sessionRepository.delete(session);
    }

    /** "2023-2027" (spaces allowed around '-'). End year after start year, at most 6 years apart. */
    private String parseSession(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Academic session is required, e.g. 2023-2027");
        }
        Matcher m = SESSION_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Academic session must look like 2023-2027");
        }
        int start = Integer.parseInt(m.group(1));
        int end = Integer.parseInt(m.group(2));
        if (start < 1950 || end > 2100) {
            throw new IllegalArgumentException("Academic session years must be between 1950 and 2100");
        }
        if (end <= start) {
            throw new IllegalArgumentException("End year must be after the start year");
        }
        if (end - start > MAX_SESSION_YEARS) {
            throw new IllegalArgumentException("Academic session can be at most " + MAX_SESSION_YEARS + " years long");
        }
        return start + "-" + end;
    }

    // =====================================================================
    // MAJOR SUBJECT
    // =====================================================================
    public List<MajorSubject> getActiveSubjects() {
        return subjectRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public List<MajorSubject> getAllSubjects() {
        return subjectRepository.findAllByOrderByNameAsc();
    }

    public MajorSubject getSubject(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Major subject not found: " + id));
    }

    @Transactional
    public MajorSubject createSubject(MajorSubjectRequest request) {
        String name = cleanName(request.getName());
        if (subjectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Major subject already exists: " + name);
        }
        MajorSubject subject = MajorSubject.builder()
                .name(name)
                .isActive(request.getActive() == null || request.getActive())
                .build();
        return subjectRepository.save(subject);
    }

    @Transactional
    public MajorSubject updateSubject(Integer id, MajorSubjectRequest request) {
        MajorSubject subject = getSubject(id);

        if (request.getName() != null) {
            String name = cleanName(request.getName());
            if (!name.equals(subject.getName())) {
                if (subjectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                    throw new DuplicateResourceException("Major subject already exists: " + name);
                }
                long used = studentRepository.countUsingMajorSubject(subject.getId(), subject.getName());
                if (used > 0) {
                    throw new ResourceInUseException("Cannot rename this major subject because "
                            + used + " student(s) use it. Create a new subject and deactivate this one instead.");
                }
                subject.setName(name);
            }
        }
        if (request.getActive() != null) {
            subject.setIsActive(request.getActive());
        }
        return subjectRepository.save(subject);
    }

    @Transactional
    public void deleteSubject(Integer id) {
        MajorSubject subject = getSubject(id);
        long used = studentRepository.countUsingMajorSubject(subject.getId(), subject.getName());
        if (used > 0) {
            throw new ResourceInUseException("Cannot delete this major subject because it is used by "
                    + used + " student(s). Deactivate it instead.");
        }
        subjectRepository.delete(subject);
    }

    private String cleanName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Major subject name is required");
        }
        String name = raw.trim().replaceAll("\\s+", " ");
        if (name.length() > 150) {
            throw new IllegalArgumentException("Major subject name can be at most 150 characters");
        }
        return name;
    }
}