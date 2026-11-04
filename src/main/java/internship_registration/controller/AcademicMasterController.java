package internship_registration.controller;

import internship_registration.dto.AcademicSessionRequest;
import internship_registration.dto.MajorSubjectRequest;
import internship_registration.dto.MessageResponse;
import internship_registration.dto.OptionResponse;
import internship_registration.entity.AcademicSession;
import internship_registration.entity.MajorSubject;
import internship_registration.service.AcademicMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Registration dropdowns.
 *  - /api/public/master/**  : no login needed (degree, department, semester, session, major subject)
 *  - /api/admin/master/**   : CRUD for academic sessions and major subjects (SUPER_ADMIN, URL rule in SecurityConfig)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicMasterController {

    private final AcademicMasterService service;

    // =====================================================================
    // PUBLIC DROPDOWNS
    // =====================================================================
    @GetMapping("/public/master/degrees")
    public ResponseEntity<List<OptionResponse>> getDegrees() {
        return ResponseEntity.ok(service.getDegrees());
    }

    // Example: /api/public/master/departments?degree=UG
    @GetMapping("/public/master/departments")
    public ResponseEntity<List<OptionResponse>> getDepartments(@RequestParam(required = false) String degree) {
        return ResponseEntity.ok(service.getDepartments(degree));
    }

    // Example: /api/public/master/semesters?degree=PG  ->  [1, 2, 3, 4]
    @GetMapping("/public/master/semesters")
    public ResponseEntity<List<Integer>> getSemesters(@RequestParam String degree) {
        return ResponseEntity.ok(service.getSemesters(degree));
    }

    @GetMapping("/public/master/academic-sessions")
    public ResponseEntity<List<AcademicSession>> getActiveSessions() {
        return ResponseEntity.ok(service.getActiveSessions());
    }

    @GetMapping("/public/master/major-subjects")
    public ResponseEntity<List<MajorSubject>> getActiveSubjects() {
        return ResponseEntity.ok(service.getActiveSubjects());
    }

    // =====================================================================
    // ADMIN: ACADEMIC SESSION
    // =====================================================================
    @GetMapping("/admin/master/academic-sessions")
    public ResponseEntity<List<AcademicSession>> getAllSessions() {
        return ResponseEntity.ok(service.getAllSessions());
    }

    @GetMapping("/admin/master/academic-sessions/{id}")
    public ResponseEntity<AcademicSession> getSession(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getSession(id));
    }

    @PostMapping("/admin/master/academic-sessions")
    public ResponseEntity<AcademicSession> createSession(@RequestBody AcademicSessionRequest request) {
        return ResponseEntity.ok(service.createSession(request));
    }

    @PutMapping("/admin/master/academic-sessions/{id}")
    public ResponseEntity<AcademicSession> updateSession(@PathVariable Integer id,
                                                         @RequestBody AcademicSessionRequest request) {
        return ResponseEntity.ok(service.updateSession(id, request));
    }

    @DeleteMapping("/admin/master/academic-sessions/{id}")
    public ResponseEntity<MessageResponse> deleteSession(@PathVariable Integer id) {
        service.deleteSession(id);
        return ResponseEntity.ok(new MessageResponse("Academic session deleted successfully"));
    }

    // =====================================================================
    // ADMIN: MAJOR SUBJECT
    // =====================================================================
    @GetMapping("/admin/master/major-subjects")
    public ResponseEntity<List<MajorSubject>> getAllSubjects() {
        return ResponseEntity.ok(service.getAllSubjects());
    }

    @GetMapping("/admin/master/major-subjects/{id}")
    public ResponseEntity<MajorSubject> getSubject(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getSubject(id));
    }

    @PostMapping("/admin/master/major-subjects")
    public ResponseEntity<MajorSubject> createSubject(@RequestBody MajorSubjectRequest request) {
        return ResponseEntity.ok(service.createSubject(request));
    }

    @PutMapping("/admin/master/major-subjects/{id}")
    public ResponseEntity<MajorSubject> updateSubject(@PathVariable Integer id,
                                                      @RequestBody MajorSubjectRequest request) {
        return ResponseEntity.ok(service.updateSubject(id, request));
    }

    @DeleteMapping("/admin/master/major-subjects/{id}")
    public ResponseEntity<MessageResponse> deleteSubject(@PathVariable Integer id) {
        service.deleteSubject(id);
        return ResponseEntity.ok(new MessageResponse("Major subject deleted successfully"));
    }
}