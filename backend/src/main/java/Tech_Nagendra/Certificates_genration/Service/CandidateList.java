package Tech_Nagendra.Certificates_genration.Service;



import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

@XmlRootElement(name = "Candidates")
@Data
public class CandidateList {
    @XmlElement(name = "Candidate")
    private List<CandidateDTO> candidates;
}