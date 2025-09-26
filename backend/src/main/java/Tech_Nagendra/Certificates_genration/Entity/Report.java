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
    private Long generatedBy;

    @Column(name = "generated_on")
    private Date generatedOn;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "course_name")
    private String courseName;

    private String level;

    @Column(name = "template_id")
    private Long templateID;

    @Column(name = "training_partner")
    private String trainingPartner;

    @Column(name = "BatchId;")
    private String BatchId;

    private String grade;
    private String templateName;
}