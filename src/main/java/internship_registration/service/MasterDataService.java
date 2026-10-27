package internship_registration.service;

import internship_registration.dto.*;
import internship_registration.entity.*;
import internship_registration.exception.DuplicateResourceException;
import internship_registration.exception.ResourceInUseException;
import internship_registration.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final UniversityRepository universityRepository;
    private final DistrictRepository districtRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipCourseRepository courseRepository;
    private final InternshipBatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final InternshipRegistrationRepository registrationRepository;
    private final WhatsappGroupRepository whatsappGroupRepository;

    // =====================================================================
    // PUBLIC GET METHODS (dropdowns)
    // =====================================================================
    public List<University> getActiveUniversities() {
        return universityRepository.findAllByIsActiveTrue();
    }

    public List<District> getAllDistricts() {
        return districtRepository.findAll();
    }

    public List<College> getCollegesForDropdown(Integer universityId, Integer districtId) {
        return collegeRepository.findByUniversityIdAndDistrictIdAndIsActiveTrue(universityId, districtId);
    }

    public List<InternshipCourse> getActiveCourses() {
        return courseRepository.findAllByIsActiveTrue();
    }

    public List<InternshipBatch> getActiveBatchesForCourse(Integer courseId) {
        return batchRepository.findByCourseIdAndIsActiveTrue(courseId);
    }

    // =====================================================================
    // UNIVERSITY
    // =====================================================================
    public List<University> getAllUniversities() {
        return universityRepository.findAll();
    }

    public University getUniversity(Integer id) {
        return findUniversity(id);
    }

    public University createUniversity(UniversityRequest request) {
        String name = requireText(request.getName(), "University name");
        String code = requireText(request.getCode(), "University code");

        if (universityRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("University already exists: " + name);
        }
        if (universityRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A university with code '" + code + "' already exists");
        }

        University university = University.builder()
                .name(name)
                .code(code)
                .isActive(true)
                .build();
        return universityRepository.save(university);
    }

    @Transactional
    public University updateUniversity(Integer id, UniversityRequest request) {
        University university = findUniversity(id);

        if (request.getName() != null) {
            String name = requireText(request.getName(), "University name");
            if (universityRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new DuplicateResourceException("University already exists: " + name);
            }
            university.setName(name);
        }
        if (request.getCode() != null) {
            String code = requireText(request.getCode(), "University code");
            if (universityRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new DuplicateResourceException("A university with code '" + code + "' already exists");
            }
            university.setCode(code);
        }
        if (request.getActive() != null) {
            university.setActive(request.getActive());
        }
        return universityRepository.save(university);
    }

    @Transactional
    public void deleteUniversity(Integer id) {
        University university = findUniversity(id);
        long colleges = collegeRepository.countByUniversityId(id);
        if (colleges > 0) {
            throw inUse("university", colleges + " college(s)", true);
        }
        universityRepository.delete(university);
    }

    // =====================================================================
    // DISTRICT
    // =====================================================================
    public District getDistrict(Integer id) {
        return findDistrict(id);
    }

    public District createDistrict(DistrictRequest request) {
        String name = requireText(request.getName(), "District name");

        if (districtRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("District already exists: " + name);
        }

        District district = District.builder()
                .name(name)
                .build();
        return districtRepository.save(district);
    }

    @Transactional
    public District updateDistrict(Integer id, DistrictRequest request) {
        District district = findDistrict(id);

        if (request.getName() != null) {
            String name = requireText(request.getName(), "District name");
            if (districtRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new DuplicateResourceException("District already exists: " + name);
            }
            district.setName(name);
        }
        return districtRepository.save(district);
    }

    @Transactional
    public void deleteDistrict(Integer id) {
        District district = findDistrict(id);
        long colleges = collegeRepository.countByDistrictId(id);
        if (colleges > 0) {
            throw inUse("district", colleges + " college(s)", false);
        }
        districtRepository.delete(district);
    }

    // =====================================================================
    // COLLEGE
    // =====================================================================
    public List<College> getAllColleges(Integer universityId, Integer districtId) {
        if (universityId != null && districtId != null) {
            return collegeRepository.findByUniversityIdAndDistrictId(universityId, districtId);
        }
        if (universityId != null) {
            return collegeRepository.findByUniversityId(universityId);
        }
        if (districtId != null) {
            return collegeRepository.findByDistrictId(districtId);
        }
        return collegeRepository.findAll();
    }

    public College getCollege(Integer id) {
        return findCollege(id);
    }

    public College createCollege(CollegeRequest request) {
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new RuntimeException("University not found"));
        District district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new RuntimeException("District not found"));

        String name = requireText(request.getName(), "College name");
        // A blank code is stored as NULL (several colleges may have no code yet, but codes can't repeat)
        String code = normalizeCode(request.getCode());

        if (collegeRepository.existsByUniversityIdAndDistrictIdAndNameIgnoreCase(
                university.getId(), district.getId(), name)) {
            throw new DuplicateResourceException("College already exists in this university and district: " + name);
        }
        if (code != null && collegeRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A college with code '" + code + "' already exists");
        }

        College college = College.builder()
                .university(university)
                .district(district)
                .name(name)
                .code(code)
                .addressLine1(request.getAddressLine1())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isActive(true)
                .build();
        return collegeRepository.save(college);
    }

    @Transactional
    public College updateCollege(Integer id, CollegeRequest request) {
        College college = findCollege(id);

        University university = college.getUniversity();
        District district = college.getDistrict();
        if (request.getUniversityId() != null) {
            university = findUniversity(request.getUniversityId());
        }
        if (request.getDistrictId() != null) {
            district = findDistrict(request.getDistrictId());
        }

        String name = request.getName() != null
                ? requireText(request.getName(), "College name")
                : college.getName();

        if (collegeRepository.existsByUniversityIdAndDistrictIdAndNameIgnoreCaseAndIdNot(
                university.getId(), district.getId(), name, id)) {
            throw new DuplicateResourceException("College already exists in this university and district: " + name);
        }

        if (request.getCode() != null) {
            String code = normalizeCode(request.getCode());
            if (code != null && collegeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new DuplicateResourceException("A college with code '" + code + "' already exists");
            }
            college.setCode(code);
        }

        college.setUniversity(university);
        college.setDistrict(district);
        college.setName(name);

        if (request.getAddressLine1() != null) college.setAddressLine1(request.getAddressLine1());
        if (request.getCity() != null) college.setCity(request.getCity());
        if (request.getState() != null) college.setState(request.getState());
        if (request.getPincode() != null) college.setPincode(request.getPincode());
        if (request.getActive() != null) college.setActive(request.getActive());

        return collegeRepository.save(college);
    }

    @Transactional
    public void deleteCollege(Integer id) {
        College college = findCollege(id);

        long students = studentRepository.countByCollegeId(id);
        if (students > 0) {
            throw inUse("college", students + " student(s)", true);
        }
        long groups = whatsappGroupRepository.countByCollegeId(id);
        if (groups > 0) {
            throw inUse("college", groups + " WhatsApp group(s)", true);
        }
        collegeRepository.delete(college);
    }

    // =====================================================================
    // COURSE
    // =====================================================================
    public List<InternshipCourse> getAllCourses() {
        return courseRepository.findAll();
    }

    public InternshipCourse getCourse(Integer id) {
        return findCourse(id);
    }

    public InternshipCourse createCourse(CourseRequest request) {
        String name = requireText(request.getName(), "Course name");
        String code = requireText(request.getCode(), "Course code");

        int theory = nonNegative(request.getTheoryDurationHours(), "Theory hours");
        int practical = nonNegative(request.getPracticalDurationHours(), "Practical hours");
        int report = nonNegative(request.getReportPreparationHours(), "Report preparation hours");
        int total = request.getTotalDurationHours() != null
                ? nonNegative(request.getTotalDurationHours(), "Total hours")
                : theory + practical + report;

        InternshipCourse course = InternshipCourse.builder()
                .name(name)
                .code(code)
                .description(request.getDescription())
                .theoryDurationHours(theory)
                .practicalDurationHours(practical)
                .reportPreparationHours(report)
                .totalDurationHours(total)
                .isActive(true)
                .build();
        return courseRepository.save(course);
    }

    @Transactional
    public InternshipCourse updateCourse(Integer id, CourseRequest request) {
        InternshipCourse course = findCourse(id);

        if (request.getName() != null) course.setName(requireText(request.getName(), "Course name"));
        if (request.getCode() != null) course.setCode(requireText(request.getCode(), "Course code"));
        if (request.getDescription() != null) course.setDescription(request.getDescription());

        boolean hoursChanged = false;
        if (request.getTheoryDurationHours() != null) {
            course.setTheoryDurationHours(nonNegative(request.getTheoryDurationHours(), "Theory hours"));
            hoursChanged = true;
        }
        if (request.getPracticalDurationHours() != null) {
            course.setPracticalDurationHours(nonNegative(request.getPracticalDurationHours(), "Practical hours"));
            hoursChanged = true;
        }
        if (request.getReportPreparationHours() != null) {
            course.setReportPreparationHours(
                    nonNegative(request.getReportPreparationHours(), "Report preparation hours"));
            hoursChanged = true;
        }
        if (request.getTotalDurationHours() != null) {
            course.setTotalDurationHours(nonNegative(request.getTotalDurationHours(), "Total hours"));
        } else if (hoursChanged) {
            // Total not sent: keep it equal to the sum of the three parts
            course.setTotalDurationHours(course.getTheoryDurationHours()
                    + course.getPracticalDurationHours()
                    + course.getReportPreparationHours());
        }

        if (request.getIsActive() != null) course.setIsActive(request.getIsActive());

        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Integer id) {
        InternshipCourse course = findCourse(id);
        long batches = batchRepository.countByCourseId(id);
        if (batches > 0) {
            throw inUse("course", batches + " batch(es)", true);
        }
        courseRepository.delete(course);
    }

    // =====================================================================
    // BATCH
    // =====================================================================
    public List<InternshipBatch> getAllBatches(Integer courseId) {
        return courseId != null ? batchRepository.findByCourseId(courseId) : batchRepository.findAll();
    }

    public InternshipBatch getBatch(Integer id) {
        return findBatch(id);
    }

    public InternshipBatch createBatch(BatchRequest request) {

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date cannot be before the start date");
        }

        InternshipCourse course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        InternshipBatch batch = InternshipBatch.builder()
                .course(course)
                .batchName(request.getBatchName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .fee(request.getFee())
                .maxStudents(request.getMaxStudents())
                .status("OPEN")
                .isActive(true)
                .build();

        return batchRepository.save(batch);
    }

    @Transactional
    public InternshipBatch updateBatch(Integer id, BatchUpdateRequest request) {
        InternshipBatch batch = findBatch(id);

        if (request.getCourseId() != null && !request.getCourseId().equals(batch.getCourse().getId())) {
            long registrations = registrationRepository.countByBatchId(id);
            if (registrations > 0) {
                throw new ResourceInUseException("Cannot move this batch to another course because it already has "
                        + registrations + " registration(s).");
            }
            batch.setCourse(findCourse(request.getCourseId()));
        }

        if (request.getBatchName() != null) batch.setBatchName(requireText(request.getBatchName(), "Batch name"));
        if (request.getStartDate() != null) batch.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) batch.setEndDate(request.getEndDate());

        if (batch.getStartDate() != null && batch.getEndDate() != null
                && batch.getEndDate().isBefore(batch.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before the start date");
        }

        if (request.getFee() != null) {
            if (request.getFee().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Fee must be greater than zero");
            }
            batch.setFee(request.getFee());
        }
        if (request.getMaxStudents() != null) {
            if (request.getMaxStudents() < 1) {
                throw new IllegalArgumentException("Maximum students must be at least 1");
            }
            batch.setMaxStudents(request.getMaxStudents());
        }
        if (request.getStatus() != null) {
            batch.setStatus(requireText(request.getStatus(), "Status").toUpperCase(Locale.ROOT));
        }
        if (request.getIsActive() != null) batch.setIsActive(request.getIsActive());

        return batchRepository.save(batch);
    }

    @Transactional
    public void deleteBatch(Integer id) {
        InternshipBatch batch = findBatch(id);

        long registrations = registrationRepository.countByBatchId(id);
        if (registrations > 0) {
            throw inUse("batch", registrations + " registration(s)", true);
        }
        long groups = whatsappGroupRepository.countByBatchId(id);
        if (groups > 0) {
            throw inUse("batch", groups + " WhatsApp group(s)", true);
        }
        batchRepository.delete(batch);
    }

    // =====================================================================
    // HELPERS
    // =====================================================================
    private University findUniversity(Integer id) {
        return universityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("University not found: " + id));
    }

    private District findDistrict(Integer id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("District not found: " + id));
    }

    private College findCollege(Integer id) {
        return collegeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("College not found: " + id));
    }

    private InternshipCourse findCourse(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + id));
    }

    private InternshipBatch findBatch(Integer id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found: " + id));
    }

    private ResourceInUseException inUse(String what, String usedBy, boolean canDeactivate) {
        return new ResourceInUseException("Cannot delete this " + what + " because it is used by " + usedBy + "."
                + (canDeactivate ? " Set it to inactive instead." : " Remove or move those first."));
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private String normalizeCode(String code) {
        return (code == null || code.isBlank()) ? null : code.trim();
    }

    private int nonNegative(Integer value, String label) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
        return value;
    }
}