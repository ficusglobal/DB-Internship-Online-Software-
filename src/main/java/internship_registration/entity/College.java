package internship_registration.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "colleges")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder

@JsonPropertyOrder({
        "id", "name", "code", "addressLine1", "city", "state", "pincode",
        "active", "createdAt", "updatedAt", "university", "district"
})
public class College extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "university_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"createdAt", "updatedAt", "createdBy", "updatedBy", "active", "isActive"})
    private University university;

    @ManyToOne
    @JoinColumn(name = "district_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"createdAt", "updatedAt", "createdBy", "updatedBy", "active", "isActive"})
    private District district;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String pincode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}