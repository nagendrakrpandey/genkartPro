package Tech_Nagendra.Certificates_genration.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "reports")
@Data
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sid;

    @Column(name = "generated_by")
    private Long generatedBy;   // kis user ne generate kiya

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "generated_on")
    private Date generatedOn;   // kab generate hua

    @Column(name = "job_role")
    private String jobrole;

    @Column(name = "course_name")
    private String courseName;

    private String level;

    @Column(name = "template_id")
    private Long templateID;

    @Column(name = "training_partner")
    private String trainingPartner;

    @Column(name = "batch_id")
    private String batchId;

    private String grade;
    private String templateName;

    // Tracking fields (manual update)
    @Column(name = "modified_by")
    private Long modifiedBy;     // kisne modify kiya

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_on")
    private Date modifiedOn;     // kab modify hua

    // optional: store count snapshot (you can also compute counts on-the-fly)
    @Column(name = "certificates_count")
    private Long certificatesCount;
}
