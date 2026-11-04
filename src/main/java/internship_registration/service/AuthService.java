package internship_registration.service;

import internship_registration.dto.*;
import internship_registration.entity.*;
import internship_registration.entity.enums.DegreeLevel;
import internship_registration.entity.enums.Department;
import internship_registration.entity.enums.Gender;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.entity.enums.Role;
import internship_registration.repository.*;
import internship_registration.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CyberCafeRepository cyberCafeRepository;
    private final StudentRepository studentRepository;
    private final InternshipRegistrationRepository internshipRegistrationRepository;
    private final InternshipBatchRepository internshipBatchRepository;
    private final CollegeRepository collegeRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final MajorSubjectRepository majorSubjectRepository;
    private final RegistrationCheckService registrationCheckService;
    private final RegistrationEmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // --- UNIVERSAL LOGIN ---
    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.getEmailOrMobile() == null || request.getEmailOrMobile().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Email/mobile and password are required");
        }

        String identifier = request.getEmailOrMobile().trim();

        // Same error for "no such user" and "wrong password" so accounts can't be probed
        User user = userRepository.findByEmailOrMobileNo(identifier, identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Throws BadCredentialsException on a wrong password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getId(), request.getPassword())
        );

        if (!user.isActive()) {
            throw new RuntimeException("Your account is disabled. Please contact support.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return generateAuthResponse(user);
    }

    // --- STUDENT REGISTRATION (email must already be verified with the OTP) ---
    @Transactional
    public AuthResponse registerStudent(StudentRegistrationRequest request) {
        // 1. Basic checks
        if (isBlank(request.getFullName())) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (isBlank(request.getPassword()) || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (request.getTermsAccepted() == null || !request.getTermsAccepted()) {
            throw new IllegalArgumentException("You must accept the Terms of Service and Privacy Policy");
        }

        String email = request.getEmail().trim();
        String mobileNo = request.getMobileNo().trim();
        String registrationNumber = blankToNull(request.getRegistrationNumber());
        String collegeRollNumber = blankToNull(request.getCollegeRollNumber());
        if (registrationNumber != null) {
            registrationNumber = RegistrationCheckService.digitsOnly(registrationNumber, "Registration number");
        }
        if (collegeRollNumber != null) {
            collegeRollNumber = RegistrationCheckService.digitsOnly(collegeRollNumber, "College roll number");
        }

        Gender gender;
        try {
            gender = Gender.valueOf(request.getGender().trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Gender must be MALE, FEMALE or OTHER");
        }

        // 2. College
        if (request.getCollegeId() == null) {
            throw new IllegalArgumentException("College is required");
        }
        College college = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new IllegalArgumentException("Selected college does not exist"));
        if (!college.isActive()) {
            throw new IllegalArgumentException("Selected college is not open for registration");
        }

        // 3. Degree (UG/PG), department of that degree, semester within the degree's range
        DegreeLevel degree = DegreeLevel.parse(request.getDegree());
        Department department = Department.parse(request.getDepartment());
        if (department.getDegree() != degree) {
            throw new IllegalArgumentException(department.getLabel() + " is not a " + degree.name() + " department");
        }
        // "Others": the student must type the department name
        String departmentOther = null;
        if (department.isOthers()) {
            departmentOther = blankToNull(request.getDepartmentOther());
            if (departmentOther == null) {
                throw new IllegalArgumentException("Please enter your department name");
            }
            if (departmentOther.length() > 150) {
                throw new IllegalArgumentException("Department name can be at most 150 characters");
            }
        }
        Integer semester = request.getSemester();
        if (semester == null || semester < 1 || semester > degree.getMaxSemester()) {
            throw new IllegalArgumentException("Semester must be between 1 and " + degree.getMaxSemester()
                    + " for " + degree.name());
        }

        // 4. Academic session and major subject from their dropdown tables
        if (request.getAcademicSessionId() == null) {
            throw new IllegalArgumentException("Academic session is required");
        }
        AcademicSession session = academicSessionRepository.findById(request.getAcademicSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Selected academic session does not exist"));
        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw new IllegalArgumentException("Selected academic session is not open for registration");
        }

        if (request.getMajorSubjectId() == null) {
            throw new IllegalArgumentException("Major subject is required");
        }
        MajorSubject subject = majorSubjectRepository.findById(request.getMajorSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Selected major subject does not exist"));
        if (!Boolean.TRUE.equals(subject.getIsActive())) {
            throw new IllegalArgumentException("Selected major subject is not available");
        }

        // 5. Batch (the fee comes from the batch)
        if (request.getBatchId() == null) {
            throw new IllegalArgumentException("Internship batch is required");
        }
        InternshipBatch batch = internshipBatchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Selected batch does not exist"));
        if (!Boolean.TRUE.equals(batch.getIsActive())) {
            throw new IllegalArgumentException("Selected batch is not open for registration");
        }
        BigDecimal batchFee = batch.getFee();

        // 6. Nothing may be registered already: email, mobile, registration no., roll no. in this college (HTTP 409)
        registrationCheckService.assertAvailable(email, mobileNo, registrationNumber, collegeRollNumber, college.getId());

        // 7. Email must have been verified with the OTP (the one-time token is used up here)
        emailVerificationService.consumeToken(request.getEmailVerificationToken(), email);

        // 8. Create login account (email already verified)
        User user = User.builder()
                .name(request.getFullName().trim())
                .email(email)
                .mobileNo(mobileNo)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .isActive(true)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        // 9. Create Student Profile (Strictly Academic Info)
        Student student = Student.builder()
                .user(user)
                .collegeId(college.getId())
                .fatherName(blankToNull(request.getFatherName()))
                .gender(gender)
                .degree(degree.name())
                .department(department.name())
                .departmentOther(departmentOther)
                .semester(semester)
                .academicSessionId(session.getId())
                .academicSession(session.getName())
                .registrationNumber(registrationNumber)
                .collegeRollNumber(collegeRollNumber)
                .majorSubjectId(subject.getId())
                .majorSubject(subject.getName())
                .isActive(true)
                .build();
        student = studentRepository.save(student);

        // 10. Cyber Cafe Detection
        CyberCafe registeringCafe = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String loggedInUserId = auth.getName();
            registeringCafe = cyberCafeRepository.findByUserId(loggedInUserId).orElse(null);
        }

        // 11. Create Internship Registration
        InternshipRegistration registration = InternshipRegistration.builder()
                .student(student)
                .cyberCafe(registeringCafe)
                .batchId(batch.getId())
                .termsAccepted(request.getTermsAccepted())
                .status(RegistrationStatus.REGISTERED)
                .courseFee(batchFee)
                .discountAmount(BigDecimal.ZERO)
                .payableAmount(batchFee)
                .isCertificateProvided(false)
                .build();
        internshipRegistrationRepository.save(registration);

        return generateAuthResponse(user);
    }

    // --- CYBER CAFE REGISTRATION ---
    @Transactional
    public AuthResponse registerCyberCafe(CyberCafeRegisterRequest request) {
        if (isBlank(request.getPassword()) || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByMobileNo(request.getMobileNo())) {
            throw new RuntimeException("Mobile number already registered");
        }

        User user = User.builder()
                .name(request.getOwnerName())
                .email(request.getEmail())
                .mobileNo(request.getMobileNo())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CYBER_CAFE)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        CyberCafe cafe = CyberCafe.builder()
                .user(user)
                .shopName(request.getShopName())
                .ownerName(request.getOwnerName())
                .address(request.getAddress())
                .isVerified(true)
                .build();
        cyberCafeRepository.save(cafe);

        return generateAuthResponse(user);
    }

    // --- ADMIN CREATION ---
    @Transactional
    public AuthResponse createUniversityAdmin(RegisterRequest request) {

        // 1. Strict Validation
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByMobileNo(request.getMobileNo())) {
            throw new RuntimeException("Mobile number already registered");
        }

        // 2. Create the exact Admin User profile
        User user = User.builder()
                .name(request.getFullName())
                .email(request.getEmail())
                .mobileNo(request.getMobileNo())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.UNIVERSITY_ADMIN)
                .isActive(true)
                .build();

        userRepository.save(user);

        return generateAuthResponse(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private AuthResponse generateAuthResponse(User user) {
        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getId(),
                        user.getPasswordHash(),
                        Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
                );
        String jwtToken = jwtService.generateToken(userDetails);
        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}