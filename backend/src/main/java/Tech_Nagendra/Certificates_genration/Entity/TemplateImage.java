package Tech_Nagendra.Certificates_genration.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "template_images")
public class TemplateImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_path")
    private String imagePath;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private Template template;

    @Column(name = "image_type")
    private Integer imageType;

}
