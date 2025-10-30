package Tech_Nagendra.Certificates_genration.Dto;
import lombok.Data;

@Data
public class CertificateDataDTO {
    private String sid;
    private Long generatedBy;
    private String jobRole;
    private String courseName;
    private String level;
    private String trainingPartner;
    private String batchId;
    private String grade;
    private String templateName;
}
