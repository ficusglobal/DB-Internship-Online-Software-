package internship_registration.service;

import internship_registration.dto.CyberCafeInfoResponse;
import internship_registration.dto.CyberCafeOverviewResponse;
import internship_registration.dto.CyberCafeSummaryResponse;
import internship_registration.dto.PageResponse;
import internship_registration.dto.StudentListFilter;
import internship_registration.dto.StudentListItemResponse;
import internship_registration.entity.*;
import internship_registration.entity.enums.Department;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.repository.CollegeRepository;
import internship_registration.repository.CyberCafeRepository;
import internship_registration.repository.InternshipBatchRepository;
import internship_registration.repository.InternshipRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Student lists with filters: the admin list, and a cyber cafe's own students. */
@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_TEXT_FILTER_LENGTH = 100;

    // ENROLLED filter = students who have paid
    public static final Set<RegistrationStatus> PAID = EnumSet.of(
            RegistrationStatus.ENROLLED, RegistrationStatus.IN_PROGRESS, RegistrationStatus.COMPLETED);
    // REGISTERED filter = registered, payment still pending
    public static final Set<RegistrationStatus> PENDING = EnumSet.of(
            RegistrationStatus.REGISTERED, RegistrationStatus.PAYMENT_PENDING);

    private final InternshipRegistrationRepository registrationRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipBatchRepository batchRepository;
    private final CyberCafeRepository cyberCafeRepository;

    // ------------------------------------------------------------------ ADMIN

    /** GET /api/admin/students */
    @Transactional(readOnly = true)
    public PageResponse<StudentListItemResponse> list(StudentListFilter f) {
        String cafeId = blankToNull(f.getCyberCafeId());
        return search(f, cafeId, registeredByFor(f.getRegisteredBy()), likePattern(f.getCyberCafeName(), "cyberCafeName"));
    }

    /**
     * GET /api/admin/cyber-cafes : every cyber cafe with its details and student counts.
     * search (optional) = part of the shop name, owner name, mobile or email.
     */
    @Transactional(readOnly = true)
    public List<CyberCafeInfoResponse> listCyberCafes(String search) {
        String q = blankToNull(search);
        if (q != null && q.length() > MAX_TEXT_FILTER_LENGTH) {
            throw new IllegalArgumentException("search must be at most " + MAX_TEXT_FILTER_LENGTH + " characters");
        }
        String needle = q == null ? null : q.toLowerCase(Locale.ROOT);

        Map<String, long[]> counts = countsPerCafe();
        List<CyberCafeInfoResponse> result = new ArrayList<>();
        for (CyberCafe cafe : cyberCafeRepository.findAllWithUser()) {
            CyberCafeInfoResponse info = withCounts(toCafeInfo(cafe), counts.getOrDefault(cafe.getId(), new long[4]));
            if (needle == null || matches(info, needle)) {
                result.add(info);
            }
        }
        // Cafes that registered the most students first
        result.sort(Comparator.comparing(CyberCafeInfoResponse::getTotalStudents).reversed()
                .thenComparing(i -> i.getShopName() == null ? "" : i.getShopName().toLowerCase(Locale.ROOT)));
        return result;
    }

    /** GET /api/admin/cyber-cafes/{id} : one cafe's details and student counts. */
    @Transactional(readOnly = true)
    public CyberCafeInfoResponse cyberCafeDetails(String cafeId) {
        CyberCafe cafe = cyberCafeRepository.findByIdWithUser(cafeId)
                .orElseThrow(() -> new NoSuchElementException("Cyber cafe not found: " + cafeId));
        long[] c = new long[4];
        for (Object[] row : registrationRepository.countByStatusForCyberCafe(cafe.getId())) {
            add(c, (RegistrationStatus) row[0], (Long) row[1]);
        }
        return withCounts(toCafeInfo(cafe), c);
    }

    /**
     * GET /api/admin/cyber-cafes/summary : how many cyber cafes there are, how many of them registered
     * students, and students registered by cafes vs. by themselves.
     */
    @Transactional(readOnly = true)
    public CyberCafeOverviewResponse cyberCafeOverview() {
        List<CyberCafe> cafes = cyberCafeRepository.findAllWithUser();
        Map<String, long[]> counts = countsPerCafe();

        long verified = 0, active = 0, withStudents = 0;
        long[] byCafes = new long[4];
        for (CyberCafe cafe : cafes) {
            if (Boolean.TRUE.equals(cafe.getIsVerified())) verified++;
            if (cafe.getUser() != null && cafe.getUser().isActive()) active++;
            long[] c = counts.get(cafe.getId());
            if (c != null && c[0] > 0) withStudents++;
        }
        for (long[] c : counts.values()) {
            for (int i = 0; i < 4; i++) byCafes[i] += c[i];
        }
        long[] self = new long[4];
        for (Object[] row : registrationRepository.countSelfRegisteredByStatus()) {
            add(self, (RegistrationStatus) row[0], (Long) row[1]);
        }

        return CyberCafeOverviewResponse.builder()
                .totalCyberCafes(cafes.size())
                .verifiedCyberCafes(verified)
                .activeCyberCafes(active)
                .cyberCafesWithStudents(withStudents)
                .cyberCafesWithoutStudents(cafes.size() - withStudents)
                .studentsByCyberCafes(toCounts(byCafes))
                .selfRegisteredStudents(toCounts(self))
                .allStudents(toCounts(new long[]{
                        byCafes[0] + self[0], byCafes[1] + self[1], byCafes[2] + self[2], byCafes[3] + self[3]}))
                .build();
    }

    // ------------------------------------------------------------------ CYBER CAFE (own students only)

    /** GET /api/cyber-cafe/summary */
    @Transactional(readOnly = true)
    public CyberCafeSummaryResponse cafeSummary(String callerUserId) {
        CyberCafe cafe = cafeOf(callerUserId);
        long[] c = new long[4];
        for (Object[] row : registrationRepository.countByStatusForCyberCafe(cafe.getId())) {
            add(c, (RegistrationStatus) row[0], (Long) row[1]);
        }
        return CyberCafeSummaryResponse.builder()
                .cyberCafeId(cafe.getId())
                .shopName(cafe.getShopName())
                .ownerName(cafe.getOwnerName())
                .isVerified(cafe.getIsVerified())
                .totalStudents(c[0])
                .paidStudents(c[1])
                .pendingStudents(c[2])
                .cancelledStudents(c[3])
                .build();
    }

    /** GET /api/cyber-cafe/students : same filters as the admin list, always limited to this cafe. */
    @Transactional(readOnly = true)
    public PageResponse<StudentListItemResponse> cafeStudents(String callerUserId, StudentListFilter f) {
        CyberCafe cafe = cafeOf(callerUserId);
        return search(f, cafe.getId(), null, null); // cyberCafeId / cyberCafeName / registeredBy sent by the cafe are ignored
    }

    // ------------------------------------------------------------------ INTERNALS

    private PageResponse<StudentListItemResponse> search(StudentListFilter f, String cafeId, String registeredBy,
                                                         String cafeNamePattern) {
        if (f.getPage() < 0) {
            throw new IllegalArgumentException("page must be 0 or more");
        }
        if (f.getSize() < 1 || f.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Page<InternshipRegistration> result = registrationRepository.searchForAdmin(
                f.getUniversityId(), f.getCollegeId(), f.getCourseId(), f.getBatchId(), statusesFor(f.getStatus()),
                likePattern(f.getName(), "name"),
                likePattern(f.getEmail(), "email"),
                likePattern(digitsOnly(f.getMobile()), "mobile"),
                likePattern(f.getSearch(), "search"),
                registeredBy, cafeId, cafeNamePattern,
                PageRequest.of(f.getPage(), f.getSize()));

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

    private CyberCafe cafeOf(String callerUserId) {
        return cyberCafeRepository.findByUserId(callerUserId)
                .orElseThrow(() -> new AccessDeniedException("Only a cyber cafe account can use this"));
    }

    /** cafeId -> [total, paid, pending, cancelled] */
    private Map<String, long[]> countsPerCafe() {
        Map<String, long[]> counts = new HashMap<>();
        for (Object[] row : registrationRepository.countByCyberCafeAndStatus()) {
            long[] c = counts.computeIfAbsent((String) row[0], k -> new long[4]);
            add(c, (RegistrationStatus) row[1], (Long) row[2]);
        }
        return counts;
    }

    private CyberCafeInfoResponse withCounts(CyberCafeInfoResponse info, long[] c) {
        info.setTotalStudents(c[0]);
        info.setPaidStudents(c[1]);
        info.setPendingStudents(c[2]);
        info.setCancelledStudents(c[3]);
        return info;
    }

    private CyberCafeOverviewResponse.Counts toCounts(long[] c) {
        return new CyberCafeOverviewResponse.Counts(c[0], c[1], c[2], c[3]);
    }

    private boolean matches(CyberCafeInfoResponse i, String needle) {
        return contains(i.getShopName(), needle) || contains(i.getOwnerName(), needle)
                || contains(i.getEmail(), needle) || contains(i.getMobileNo(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** counts = [total, paid, pending, cancelled] */
    private void add(long[] counts, RegistrationStatus status, Long n) {
        long value = n == null ? 0 : n;
        counts[0] += value;
        if (PAID.contains(status)) counts[1] += value;
        else if (status == RegistrationStatus.CANCELLED) counts[3] += value;
        else counts[2] += value;
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

    /** null = everyone; SELF = registered alone; CYBER_CAFE = registered by any cafe. */
    private String registeredByFor(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("ALL")) {
            return null;
        }
        String v = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        switch (v) {
            case "SELF":
            case "STUDENT":
                return "SELF";
            case "CYBER_CAFE":
            case "CAFE":
                return "CYBER_CAFE";
            default:
                throw new IllegalArgumentException("registeredBy must be SELF, CYBER_CAFE or ALL");
        }
    }

    /** "Ravi " -> "%ravi%" for a contains-search; % and _ typed by the user are matched literally. */
    private String likePattern(String value, String field) {
        String v = blankToNull(value);
        if (v == null) {
            return null;
        }
        if (v.length() > MAX_TEXT_FILTER_LENGTH) {
            throw new IllegalArgumentException(field + " must be at most " + MAX_TEXT_FILTER_LENGTH + " characters");
        }
        String escaped = v.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    /** Mobile filter: keep digits only, so "+91 98765-43210" still finds 9876543210. */
    private String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() > 10 && digits.startsWith("91")) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits.isEmpty() ? null : digits;
    }

    private static boolean hasPhoto(Student s) {
        return s.getPhotoUrl() != null && !s.getPhotoUrl().isBlank();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String paymentStatus(RegistrationStatus status) {
        if (status == null) return null;
        if (PAID.contains(status)) return "PAID";
        if (status == RegistrationStatus.CANCELLED) return "CANCELLED";
        return "PENDING";
    }

    private CyberCafeInfoResponse toCafeInfo(CyberCafe cafe) {
        User cu = cafe.getUser();
        return CyberCafeInfoResponse.builder()
                .cyberCafeId(cafe.getId())
                .shopName(cafe.getShopName())
                .ownerName(cafe.getOwnerName())
                .mobileNo(cu != null ? cu.getMobileNo() : null)
                .email(cu != null ? cu.getEmail() : null)
                .address(cafe.getAddress())
                .isVerified(cafe.getIsVerified())
                .isActive(cu != null ? cu.isActive() : null)
                .joinedAt(cafe.getCreatedAt())
                .build();
    }

    private StudentListItemResponse toItem(InternshipRegistration r, College college, InternshipBatch batch) {
        Student s = r.getStudent();
        User u = s.getUser();
        University university = college != null ? college.getUniversity() : null;
        InternshipCourse course = batch != null ? batch.getCourse() : null;
        CyberCafe cafe = r.getCyberCafe();

        return StudentListItemResponse.builder()
                // registration
                .registrationId(r.getId())
                .registrationStatus(r.getStatus() != null ? r.getStatus().name() : null)
                .paymentStatus(paymentStatus(r.getStatus()))
                .payableAmount(r.getPayableAmount())
                .consentStatus(r.getConsentStatus() != null ? r.getConsentStatus().name() : null)
                .registeredAt(r.getCreatedAt())
                .registeredBy(cafe != null ? "CYBER_CAFE" : "SELF")
                .registeredByCyberCafe(cafe != null ? cafe.getShopName() : null)
                .cyberCafe(cafe != null ? toCafeInfo(cafe) : null)
                // student
                .studentId(s.getId())
                .studentName(u != null ? u.getName() : null)
                .hasPhoto(hasPhoto(s))
                .photoUrl(hasPhoto(s) ? StudentPhotoService.photoPath(r.getId()) : null)
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