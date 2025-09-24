package Tech_Nagendra.Certificates_genration.Entity;


import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

import java.util.List;

@XmlRootElement(name = "Candidate")
@XmlType(propOrder = {
        "salutation","candidateName","sid","courseName","guardianType","guardianName",
        "sectorSkillCouncil","dateOfIssuance","level","aadhaarNumber","sector","grade",
        "dateOfStart","district","state","fatherORHusbandName","dateColumn1","column2","column3","column4","column5",
        "column6","column7","column8","column9","column10","additionalColumns"
})
@Data
public class Candidate {
    private String salutation;
    private String candidateName;
    private String sid;
    private String courseName;
    private String guardianType;
    private String guardianName; // simplified
    private String sectorSkillCouncil;
    private String dateOfIssuance;
    private String level;
    private String aadhaarNumber;
    private String sector;
    private String grade;
    private String dateOfStart;
    private String district;
    private String state;
    private String fatherORHusbandName;
    private String dateColumn1;
    private String column2;
    private String column3;
    private String column4;
    private String column5;
    private String column6;
    private String column7;
    private String column8;
    private String column9;
    private String column10;
    private List<String> additionalColumns;
}
