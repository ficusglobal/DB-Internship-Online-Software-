package internship_registration.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String fullName;
    private String email;
    private String mobileNo;
    private String password;
}