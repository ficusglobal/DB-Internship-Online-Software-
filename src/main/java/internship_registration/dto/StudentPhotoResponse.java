package internship_registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Result of uploading (or checking) a student's photo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPhotoResponse {
    private String registrationId;
    private String studentId;
    private boolean hasPhoto;
    private String photoUrl;             // GET this with the JWT to show the image; null when there is no photo
    private LocalDateTime photoUploadedAt;
    private boolean canChange;           // false once the acceptance letter is issued (only an admin can change it then)
}