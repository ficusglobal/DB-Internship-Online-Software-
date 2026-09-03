package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "districts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;
}