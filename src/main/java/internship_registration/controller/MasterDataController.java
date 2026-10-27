package internship_registration.controller;

import internship_registration.dto.*;
import internship_registration.entity.*;
import internship_registration.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Master data: universities, districts, colleges, courses, batches.
 *  - /api/public/master/**  : read-only dropdowns, no login needed
 *  - /api/admin/master/**   : full CRUD for SUPER_ADMIN (the URL rule in SecurityConfig enforces the role)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    // =====================================================================
    // PUBLIC (dropdowns)
    // =====================================================================
    @GetMapping("/public/master/universities")
    public ResponseEntity<List<University>> getUniversities() {
        return ResponseEntity.ok(masterDataService.getActiveUniversities());
    }

    @GetMapping("/public/master/districts")
    public ResponseEntity<List<District>> getDistricts() {
        return ResponseEntity.ok(masterDataService.getAllDistricts());
    }

    @GetMapping("/public/master/colleges")
    public ResponseEntity<List<College>> getColleges(
            @RequestParam Integer universityId,
            @RequestParam Integer districtId) {
        // Example Frontend Call: /api/public/master/colleges?universityId=1&districtId=5
        return ResponseEntity.ok(masterDataService.getCollegesForDropdown(universityId, districtId));
    }

    @GetMapping("/public/master/courses")
    public ResponseEntity<List<InternshipCourse>> getCourses() {
        return ResponseEntity.ok(masterDataService.getActiveCourses());
    }

    @GetMapping("/public/master/batches")
    public ResponseEntity<List<InternshipBatch>> getBatches(@RequestParam Integer courseId) {
        return ResponseEntity.ok(masterDataService.getActiveBatchesForCourse(courseId));
    }

    // =====================================================================
    // ADMIN: UNIVERSITY
    // =====================================================================
    @GetMapping("/admin/master/universities")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<University>> getAllUniversities() {
        return ResponseEntity.ok(masterDataService.getAllUniversities());
    }

    @GetMapping("/admin/master/universities/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<University> getUniversity(@PathVariable Integer id) {
        return ResponseEntity.ok(masterDataService.getUniversity(id));
    }

    @PostMapping("/admin/master/universities")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<University> createUniversity(@RequestBody UniversityRequest request) {
        return ResponseEntity.ok(masterDataService.createUniversity(request));
    }

    @PutMapping("/admin/master/universities/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<University> updateUniversity(@PathVariable Integer id,
                                                       @RequestBody UniversityRequest request) {
        return ResponseEntity.ok(masterDataService.updateUniversity(id, request));
    }

    @DeleteMapping("/admin/master/universities/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteUniversity(@PathVariable Integer id) {
        masterDataService.deleteUniversity(id);
        return ResponseEntity.ok(new MessageResponse("University deleted successfully"));
    }

    // =====================================================================
    // ADMIN: DISTRICT
    // =====================================================================
    @GetMapping("/admin/master/districts")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<District>> getAllDistricts() {
        return ResponseEntity.ok(masterDataService.getAllDistricts());
    }

    @GetMapping("/admin/master/districts/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<District> getDistrict(@PathVariable Integer id) {
        return ResponseEntity.ok(masterDataService.getDistrict(id));
    }

    @PostMapping("/admin/master/districts")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<District> createDistrict(@RequestBody DistrictRequest request) {
        return ResponseEntity.ok(masterDataService.createDistrict(request));
    }

    @PutMapping("/admin/master/districts/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<District> updateDistrict(@PathVariable Integer id,
                                                   @RequestBody DistrictRequest request) {
        return ResponseEntity.ok(masterDataService.updateDistrict(id, request));
    }

    @DeleteMapping("/admin/master/districts/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteDistrict(@PathVariable Integer id) {
        masterDataService.deleteDistrict(id);
        return ResponseEntity.ok(new MessageResponse("District deleted successfully"));
    }

    // =====================================================================
    // ADMIN: COLLEGE
    // =====================================================================
    @GetMapping("/admin/master/colleges")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<College>> getAllColleges(
            @RequestParam(required = false) Integer universityId,
            @RequestParam(required = false) Integer districtId) {
        return ResponseEntity.ok(masterDataService.getAllColleges(universityId, districtId));
    }

    @GetMapping("/admin/master/colleges/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<College> getCollege(@PathVariable Integer id) {
        return ResponseEntity.ok(masterDataService.getCollege(id));
    }

    @PostMapping("/admin/master/colleges")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<College> createCollege(@RequestBody CollegeRequest request) {
        return ResponseEntity.ok(masterDataService.createCollege(request));
    }

    @PutMapping("/admin/master/colleges/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<College> updateCollege(@PathVariable Integer id,
                                                 @RequestBody CollegeRequest request) {
        return ResponseEntity.ok(masterDataService.updateCollege(id, request));
    }

    @DeleteMapping("/admin/master/colleges/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteCollege(@PathVariable Integer id) {
        masterDataService.deleteCollege(id);
        return ResponseEntity.ok(new MessageResponse("College deleted successfully"));
    }

    // =====================================================================
    // ADMIN: COURSE
    // =====================================================================
    @GetMapping("/admin/master/courses")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<InternshipCourse>> getAllCourses() {
        return ResponseEntity.ok(masterDataService.getAllCourses());
    }

    @GetMapping("/admin/master/courses/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipCourse> getCourse(@PathVariable Integer id) {
        return ResponseEntity.ok(masterDataService.getCourse(id));
    }

    @PostMapping("/admin/master/courses")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipCourse> createCourse(@RequestBody CourseRequest request) {
        return ResponseEntity.ok(masterDataService.createCourse(request));
    }

    @PutMapping("/admin/master/courses/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipCourse> updateCourse(@PathVariable Integer id,
                                                         @RequestBody CourseRequest request) {
        return ResponseEntity.ok(masterDataService.updateCourse(id, request));
    }

    @DeleteMapping("/admin/master/courses/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteCourse(@PathVariable Integer id) {
        masterDataService.deleteCourse(id);
        return ResponseEntity.ok(new MessageResponse("Course deleted successfully"));
    }

    // =====================================================================
    // ADMIN: BATCH
    // =====================================================================
    @GetMapping("/admin/master/batches")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<InternshipBatch>> getAllBatches(
            @RequestParam(required = false) Integer courseId) {
        return ResponseEntity.ok(masterDataService.getAllBatches(courseId));
    }

    @GetMapping("/admin/master/batches/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipBatch> getBatch(@PathVariable Integer id) {
        return ResponseEntity.ok(masterDataService.getBatch(id));
    }

    @PostMapping("/admin/master/batches")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipBatch> createBatch(@RequestBody BatchRequest request) {
        return ResponseEntity.ok(masterDataService.createBatch(request));
    }

    @PutMapping("/admin/master/batches/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipBatch> updateBatch(@PathVariable Integer id,
                                                       @RequestBody BatchUpdateRequest request) {
        return ResponseEntity.ok(masterDataService.updateBatch(id, request));
    }

    @DeleteMapping("/admin/master/batches/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> deleteBatch(@PathVariable Integer id) {
        masterDataService.deleteBatch(id);
        return ResponseEntity.ok(new MessageResponse("Batch deleted successfully"));
    }
}