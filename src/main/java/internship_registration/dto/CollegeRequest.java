package internship_registration.dto;

import lombok.Data;

@Data
public class CollegeRequest {
    private Integer universityId;
    private Integer districtId;
    private String name;
    private String code;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
}
