package internship_registration.service;

import internship_registration.dto.StudentProfileDetailsResponse;
import internship_registration.entity.*;
import internship_registration.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipRegistrationRepository registrationRepository;
    private final InternshipBatchRepository batchRepository;

    @Transactional(readOnly = true)
    public StudentProfileDetailsResponse getStudentDetails(String studentOrUserId) {
        // 1. Resolve Student (either by student primary ID or linked user ID)
        Student student = studentRepository.findById(studentOrUserId)
                .or(() -> studentRepository.findByUser_Id(studentOrUserId))
                .orElseThrow(() -> new IllegalArgumentException("Student not found for identifier: " + studentOrUserId));

        // 2. Fetch User safely from relationship
        User user = student.getUser();

        // 3. Fetch College using collegeId
        College college = null;
        if (student.getCollegeId() != null) {
            college = collegeRepository.findById(student.getCollegeId()).orElse(null);
        }

        // 4. Fetch Registration and Batch
        InternshipRegistration registration = registrationRepository
                .findFirstByStudent_IdOrderByCreatedAtDesc(student.getId())
                .orElse(null);

        InternshipBatch batch = null;
        InternshipCourse course = null;

        if (registration != null && registration.getBatchId() != null) {
            batch = batchRepository.findById(registration.getBatchId()).orElse(null);
            if (batch != null) {
                course = batch.getCourse();
            }
        }

        // 5. Build college address (College entity has addressLine1)
        String collegeAddress = (college != null && college.getAddressLine1() != null)
                ? college.getAddressLine1()
                : "";

        // 6. Return response
        return StudentProfileDetailsResponse.builder()
                .studentId(student.getId())
                .studentName(user != null ? user.getName() : null)
                .email(user != null ? user.getEmail() : null)
                .mobileNo(user != null ? user.getMobileNo() : null)
                .fatherName(student.getFatherName())
                .gender(student.getGender() != null ? student.getGender().name() : null)
                .studentRegistrationNumber(student.getRegistrationNumber())
                .collegeRollNumber(student.getCollegeRollNumber())
                .degree(student.getDegree())
                .department(student.getDepartment())
                .academicSession(student.getAcademicSession())
                // College details
                .collegeId(student.getCollegeId())
                .collegeName(college != null ? college.getName() : null)
                .collegeCode(college != null ? college.getCode() : null)
                .collegeAddress(collegeAddress)
                .city(college != null ? college.getCity() : null)
                .state(college != null ? college.getState() : null)
                .pincode(college != null ? college.getPincode() : null)
                // Registration details
                .registrationId(registration != null ? registration.getId() : null)
                .internshipRegistrationNumber(registration != null ? registration.getRegistrationNumber() : null)
                .registrationStatus(registration != null && registration.getStatus() != null ? registration.getStatus().name() : null)
                .batchId(batch != null ? batch.getId() : null)
                .batchName(batch != null ? batch.getBatchName() : null)
                .courseName(course != null ? course.getName() : null)
                .build();
    }
}