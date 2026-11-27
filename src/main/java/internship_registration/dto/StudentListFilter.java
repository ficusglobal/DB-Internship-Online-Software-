package internship_registration.dto;

import lombok.Data;


@Data
public class StudentListFilter {
    private Integer universityId;
    private Integer collegeId;
    private Integer courseId;
    private Integer batchId;
    private String status;
    private String name;
    private String email;
    private String mobile;
    private String search;
    private String registeredBy;
    private String cyberCafeId;
    private String cyberCafeName;
    private int page = 0;
    private int size = 20;
}