package internship_registration.entity;

import jakarta.persistence.*;
import lombok.*;

/** One WhatsApp group (invite link) per college and batch. */
@Entity
@Table(name = "whatsapp_groups")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "batch_id", nullable = false)
    private Integer batchId;

    @Column(name = "college_id", nullable = false)
    private Integer collegeId;

    @Column(name = "group_name", nullable = false, length = 255)
    private String groupName;

    @Column(name = "group_url", nullable = false, length = 1000)
    private String groupUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}