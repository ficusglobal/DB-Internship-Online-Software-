package internship_registration.controller;

import internship_registration.dto.*;
import internship_registration.entity.*;
import internship_registration.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;



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



    @PostMapping("/admin/master/universities")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<University> createUniversity(@RequestBody UniversityRequest request) {
        return ResponseEntity.ok(masterDataService.createUniversity(request));
    }

    @PostMapping("/admin/master/districts")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<District> createDistrict(@RequestBody DistrictRequest request) {
        return ResponseEntity.ok(masterDataService.createDistrict(request));
    }

    @PostMapping("/admin/master/colleges")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<College> createCollege(@RequestBody CollegeRequest request) {
        return ResponseEntity.ok(masterDataService.createCollege(request));
    }


    @GetMapping("/public/master/courses")
    public ResponseEntity<List<InternshipCourse>> getCourses() {
        return ResponseEntity.ok(masterDataService.getActiveCourses());
    }

    @GetMapping("/public/master/batches")
    public ResponseEntity<List<InternshipBatch>> getBatches(@RequestParam Integer courseId) {
        return ResponseEntity.ok(masterDataService.getActiveBatchesForCourse(courseId));
    }

    @PostMapping("/admin/master/courses")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipCourse> createCourse(@RequestBody CourseRequest request) {
        return ResponseEntity.ok(masterDataService.createCourse(request));
    }

    @PostMapping("/admin/master/batches")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<InternshipBatch> createBatch(@RequestBody BatchRequest request) {
        return ResponseEntity.ok(masterDataService.createBatch(request));
    }
}