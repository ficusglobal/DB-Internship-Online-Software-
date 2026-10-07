package internship_registration.service;

import internship_registration.dto.*;
import internship_registration.entity.*;
import internship_registration.exception.DuplicateResourceException;
import internship_registration.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final UniversityRepository universityRepository;
    private final DistrictRepository districtRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipCourseRepository courseRepository;
    private final InternshipBatchRepository batchRepository;

    // --- GET METHODS FOR UNIVERSITY, DISTRICT AND COLLEGE ---
    public List<University> getActiveUniversities() {
        return universityRepository.findAllByIsActiveTrue();
    }

    public List<District> getAllDistricts() {
        return districtRepository.findAll();
    }

    public List<College> getCollegesForDropdown(Integer universityId, Integer districtId) {
        return collegeRepository.findByUniversityIdAndDistrictIdAndIsActiveTrue(universityId, districtId);
    }

    // --- POST METHODS FOR ADMIN TO CREATE UNIVERSITY, DISTRICT AND COLLEGE ---
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

    public College createCollege(CollegeRequest request) {
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new RuntimeException("University not found"));
        District district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new RuntimeException("District not found"));

        String name = requireText(request.getName(), "College name");
        // A blank code is stored as NULL (several colleges may have no code yet, but codes can't repeat)
        String code = (request.getCode() == null || request.getCode().isBlank()) ? null : request.getCode().trim();

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

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    // --- GET METHODS FOR COURSES AND BATCHES ---
    public List<InternshipCourse> getActiveCourses() {
        return courseRepository.findAllByIsActiveTrue();
    }

    public List<InternshipBatch> getActiveBatchesForCourse(Integer courseId) {
        return batchRepository.findByCourseIdAndIsActiveTrue(courseId);
    }

    // --- POST METHODS AMIN TO CREATE COURSE AND BATCH ---
    public InternshipCourse createCourse(CourseRequest request) {
        InternshipCourse course = InternshipCourse.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .theoryDurationHours(request.getTheoryDurationHours())
                .practicalDurationHours(request.getPracticalDurationHours())
                .reportPreparationHours(request.getReportPreparationHours())
                .totalDurationHours(request.getTotalDurationHours())
                .isActive(true)
                .build();
        return courseRepository.save(course);
    }

    public InternshipBatch createBatch(BatchRequest request) {

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
}