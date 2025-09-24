package Tech_Nagendra.Certificates_genration.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private Long id;   // changed from int → Long
    private String name;
    private String username;
    private String email;
    private String role;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    // Users
    private Long  createdBy;
    private Long modifiedBy;
}