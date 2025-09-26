package Tech_Nagendra.Certificates_genration.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "candidates")
public class CandidateDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String salutation;
    private String candidateName;
    private String sid;
    private String jobrole;
    private String guardianType;
    private String fatherORHusbandName;
    private String sectorSkillCouncil;
    private String dateOfIssuance;
    private String nsqfLevel;
    private String aadhaarNumber; // fixed name
    private String sector;
    private String grade;
    private String dateOfStart;
    private String dateOfEnd;
    private String marks;
    private String marks1;
    private String marks2;
    private String marks3;
    private String batchId; // fixed name

    @ManyToOne
    @JoinColumn(name="template_id")
    private Template template; // optional: link candidate with template
}
