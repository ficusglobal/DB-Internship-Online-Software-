package internship_registration.service;

import internship_registration.dto.PageResponse;
import internship_registration.dto.StudentListItemResponse;
import internship_registration.entity.*;
import internship_registration.entity.enums.Department;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.repository.CollegeRepository;
import internship_registration.repository.InternshipBatchRepository;
import internship_registration.repository.InternshipRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Admin list of registered students with filters. */
@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private static final int MAX_PAGE_SIZE = 200;

    // ENROLLED filter = students who have paid
    public static final Set<RegistrationStatus> PAID = EnumSet.of(
            RegistrationStatus.ENROLLED, RegistrationStatus.IN_PROGRESS, RegistrationStatus.COMPLETED);
    // REGISTERED filter = registered, payment still pending
    public static final Set<RegistrationStatus> PENDING = EnumSet.of(
            RegistrationStatus.REGISTERED, RegistrationStatus.PAYMENT_PENDING);

    private final InternshipRegistrationRepository registrationRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipBatchRepository batchRepository;

    /**
     * @param status ENROLLED (paid), REGISTERED (payment pending), CANCELLED, or null/ALL for everyone
     */
    @Transactional(readOnly = true)
    public PageResponse<StudentListItemResponse> list(Integer universityId, Integer collegeId, Integer courseId,
                                                      Integer batchId, String status, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or more");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Page<InternshipRegistration> result = registrationRepository.searchForAdmin(
                universityId, collegeId, courseId, batchId, statusesFor(status), PageRequest.of(page, size));

        List<InternshipRegistration> rows = result.getContent();

        // Load colleges and batches once for the whole page (no query per row)
        Set<Integer> collegeIds = new HashSet<>();
        Set<Integer> batchIds = new HashSet<>();
        for (InternshipRegistration r : rows) {
            collegeIds.add(r.getStudent().getCollegeId());
            batchIds.add(r.getBatchId());
        }
        Map<Integer, College> colleges = new HashMap<>();
        for (College c : collegeRepository.findAllById(collegeIds)) {
            colleges.put(c.getId(), c);
        }
        Map<Integer, InternshipBatch> batches = new HashMap<>();
        for (InternshipBatch b : batchRepository.findAllById(batchIds)) {
            batches.put(b.getId(), b);
        }

        List<StudentListItemResponse> content = new ArrayList<>();
        for (InternshipRegistration r : rows) {
            content.add(toItem(r, colleges.get(r.getStudent().getCollegeId()), batches.get(r.getBatchId())));
        }

        return PageResponse.<StudentListItemResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private Set<RegistrationStatus> statusesFor(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return EnumSet.allOf(RegistrationStatus.class);
        }
        switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ENROLLED":
            case "PAID":
                return PAID;
            case "REGISTERED":
            case "PENDING":
                return PENDING;
            case "CANCELLED":
                return EnumSet.of(RegistrationStatus.CANCELLED);
            default:
                throw new IllegalArgumentException("status must be ENROLLED, REGISTERED, CANCELLED or ALL");
        }
    }

    private String paymentStatus(RegistrationStatus status) {
        if (status == null) return null;
        if (PAID.contains(status)) return "PAID";
        if (status == RegistrationStatus.CANCELLED) return "CANCELLED";
        return "PENDING";
    }

    private StudentListItemResponse toItem(InternshipRegistration r, College college, InternshipBatch batch) {
        Student s = r.getStudent();
        User u = s.getUser();
        University university = college != null ? college.getUniversity() : null;
        InternshipCourse course = batch != null ? batch.getCourse() : null;

        return StudentListItemResponse.builder()
                // registration
                .registrationId(r.getId())
                .registrationStatus(r.getStatus() != null ? r.getStatus().name() : null)
                .paymentStatus(paymentStatus(r.getStatus()))
                .payableAmount(r.getPayableAmount())
                .consentStatus(r.getConsentStatus() != null ? r.getConsentStatus().name() : null)
                .registeredAt(r.getCreatedAt())
                .registeredByCyberCafe(r.getCyberCafe() != null ? r.getCyberCafe().getShopName() : null)
                // student
                .studentId(s.getId())
                .studentName(u != null ? u.getName() : null)
                .email(u != null ? u.getEmail() : null)
                .mobileNo(u != null ? u.getMobileNo() : null)
                .fatherName(s.getFatherName())
                .gender(s.getGender() != null ? s.getGender().name() : null)
                .registrationNumber(s.getRegistrationNumber())
                .collegeRollNumber(s.getCollegeRollNumber())
                .degree(s.getDegree())
                .department(Department.display(s.getDepartment(), s.getDepartmentOther()))
                .semester(s.getSemester())
                .academicSession(s.getAcademicSession())
                .majorSubject(s.getMajorSubject())
                // college / university
                .collegeId(s.getCollegeId())
                .collegeName(college != null ? college.getName() : null)
                .collegeCode(college != null ? college.getCode() : null)
                .universityId(university != null ? university.getId() : null)
                .universityName(university != null ? university.getName() : null)
                // internship
                .batchId(r.getBatchId())
                .batchName(batch != null ? batch.getBatchName() : null)
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .build();
    }
}