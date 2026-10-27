package internship_registration.security;

import internship_registration.entity.InternshipRegistration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Small shared checks for "who may touch this registration". */
public final class AccessUtils {

    private AccessUtils() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("SUPER_ADMIN") || a.equals("UNIVERSITY_ADMIN"));
    }

    /** The student who owns the registration, or the cyber cafe that registered the student. */
    public static boolean canAccess(InternshipRegistration reg, String userId) {
        if (reg == null || userId == null) {
            return false;
        }
        boolean isOwner = reg.getStudent() != null && reg.getStudent().getUser() != null
                && userId.equals(reg.getStudent().getUser().getId());
        boolean isRegisteringCafe = reg.getCyberCafe() != null && reg.getCyberCafe().getUser() != null
                && userId.equals(reg.getCyberCafe().getUser().getId());
        return isOwner || isRegisteringCafe;
    }
}