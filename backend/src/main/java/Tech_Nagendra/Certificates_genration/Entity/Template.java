package Tech_Nagendra.Certificates_genration.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EntityListeners(AuditingEntityListener.class)
@Table(name = "templates")
public class Template {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String templateName;
//    private String userId;
//
//    private Integer imageType;
//
//    @CreatedDate
//    private LocalDateTime createdAt;
//
//    @CreatedBy
//    private String createdBy;
//
//    @LastModifiedDate
//    private LocalDateTime modifiedAt;
//
//    @LastModifiedBy
//    private String modifiedBy;
//
//    @Column(name = "jrxml_file")
//    private String  jrxmlFile;
//
//    @ElementCollection
//    @Column(name = "images")
//    private List<String> images;
//}


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String templateName;

    private Integer imageType;

    private long user_id;

    private Long createdBy; // user id who uploaded

    @CreatedDate
    private LocalDateTime createdAt;

    private String jrxmlPath;

    @ElementCollection
    @CollectionTable(name = "template_images", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "image_path")
    private List<String> imagePaths = new ArrayList<>();

    private String templateFolder; // folder path

    @LastModifiedDate
   private LocalDateTime modifiedAt;

   @LastModifiedBy
    private String modifiedBy;
}