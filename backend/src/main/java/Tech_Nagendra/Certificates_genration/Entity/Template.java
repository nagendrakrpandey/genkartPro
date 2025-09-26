package Tech_Nagendra.Certificates_genration.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Data
@EntityListeners(AuditingEntityListener.class)
@Table(name = "templates")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "image_type")
    private Integer imageType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_by")
    private Long createdBy; // user id who uploaded

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "jrxml_path")
    private String jrxmlPath;

    @Column(name = "template_folder")
    private String templateFolder;

    // CSV string in DB
    @Column(name = "image_paths")
    private String imagePaths;

    @Column(name = "common_images")
    private String commonImages;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    // Convert CSV to List
    public List<String> getImagePathsList() {
        if (imagePaths == null || imagePaths.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(imagePaths.split(",")));
    }

    public List<String> getCommonImagesList() {
        if (commonImages == null || commonImages.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(commonImages.split(",")));
    }
}
