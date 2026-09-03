package internship_registration.service;

import internship_registration.dto.*;
import internship_registration.entity.*;
import internship_registration.entity.enums.Gender;
import internship_registration.entity.enums.RegistrationStatus;
import internship_registration.entity.enums.Role;
import internship_registration.repository.*;
import internship_registration.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CyberCafeRepository cyberCafeRepository;
    private final StudentRepository studentRepository;
    private final InternshipRegistrationRepository internshipRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // --- UNIVERSAL LOGIN ---
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrMobileNo(request.getEmailOrMobile(), request.getEmailOrMobile())
                .orElseThrow(() -> new RuntimeException("User not found"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getId(), request.getPassword())
        );

        return generateAuthResponse(user);
    }

    // --- UNIFIED STUDENT REGISTRATION ---
    @Transactional
    public AuthResponse registerStudent(StudentRegistrationRequest request) {
        // 1. Strict Validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByMobileNo(request.getMobileNo())) {
            throw new RuntimeException("Mobile number already registered");
        }
        if (request.getTermsAccepted() == null || !request.getTermsAccepted()) {
            throw new RuntimeException("You must accept the Terms of Service and Privacy Policy");
        }

        // 2. Create Login Account
        User user = User.builder()
                .name(request.getFullName())
                .email(request.getEmail())
                .mobileNo(request.getMobileNo())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        // 3. Create Student Profile (Strictly Academic Info)
        Student student = Student.builder()
                .user(user)
                .collegeId(request.getCollegeId())
                .fatherName(request.getFatherName())
                .gender(Gender.valueOf(request.getGender().toUpperCase()))
                .degree(request.getDegree())
                .department(request.getDepartment())
                .academicSession(request.getAcademicSession())
                .registrationNumber(request.getRegistrationNumber())
                .collegeRollNumber(request.getCollegeRollNumber())
                .majorSubject(request.getMajorSubject())
                .isActive(true)
                .build();
        student = studentRepository.save(student);

        // 4. Cyber Cafe Detection
        CyberCafe registeringCafe = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String loggedInUserId = auth.getName();
            registeringCafe = cyberCafeRepository.findByUserId(loggedInUserId).orElse(null);
        }

        // 5. Create Internship Registration
        InternshipRegistration registration = InternshipRegistration.builder()
                .student(student)
                .cyberCafe(registeringCafe)
                .batchId(request.getBatchId())
                .termsAccepted(request.getTermsAccepted())
                .status(RegistrationStatus.REGISTERED)
                .courseFee(BigDecimal.valueOf(5000.00))
                .discountAmount(BigDecimal.ZERO)
                .payableAmount(BigDecimal.valueOf(5000.00))
                .isCertificateProvided(false)
                .build();
        internshipRegistrationRepository.save(registration);

        return generateAuthResponse(user);
    }

    // --- CYBER CAFE REGISTRATION ---
    @Transactional
    public AuthResponse registerCyberCafe(CyberCafeRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
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