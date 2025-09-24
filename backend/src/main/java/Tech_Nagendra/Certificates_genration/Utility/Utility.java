package Tech_Nagendra.Certificates_genration.Utility;

import Tech_Nagendra.Certificates_genration.Entity.Candidate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Utility {

    public static <T> String convertObjectToXML(Object object, Class<T> classOb) {
        StringWriter sw = new StringWriter();
        try {
            JAXBContext contextObj = JAXBContext.newInstance(classOb);
            Marshaller marshallerObj = contextObj.createMarshaller();
            marshallerObj.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
            marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshallerObj.marshal(object, sw);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertXMLToObject(String xml, Class<T> classOb) {
        Object returnOb = null;
        try {
            StringReader sr = new StringReader(xml);
            JAXBContext jaxbContext = JAXBContext.newInstance(classOb);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            returnOb = jaxbUnmarshaller.unmarshal(sr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) returnOb;
    }

    public static List<Candidate> getCandidatesFromFile(File file) {
        List<Candidate> candidates = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(file))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (!"Candidate Data".equals(sheet.getSheetName())) {
                throw new RuntimeException("Sheet name must be 'Candidate Data'");
            }
            DataFormatter dataFormatter = new DataFormatter();
            int rowCount = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
            for (Row row : sheet) {
                if (rowCount++ == 0) continue;
                Candidate candidate = new Candidate();
                int columnCount = 0;
                for (Cell cell : row) {
                    String cellValue = dataFormatter.formatCellValue(cell);
                    switch (columnCount) {
                        case 0 -> candidate.setSalutation(cellValue);
                        case 1 -> candidate.setCandidateName(cellValue);
                        case 2 -> candidate.setSid(cellValue);
                        case 3 -> candidate.setCourseName(cellValue);
                        case 4 -> candidate.setGuardianType(cellValue);
                        case 5 -> candidate.setFatherORHusbandName(cellValue);
                        case 6 -> candidate.setSectorSkillCouncil(cellValue);
                        case 7 -> {
                            try { candidate.setDateOfIssuance(cellValue); } catch (Exception ignored) {}
                        }
                        case 8 -> candidate.setLevel(cellValue);
                        case 9 -> candidate.setAadhaarNumber(cellValue);
                        case 10 -> candidate.setSector(cellValue);
                        case 11 -> candidate.setGrade(cellValue);
                        case 12 -> {
                            try { candidate.setDateOfStart(cellValue); } catch (Exception ignored) {}
                        }
                        case 13 -> candidate.setDistrict(cellValue);
                        case 14 -> candidate.setState(cellValue);
                        case 15 -> { candidate.setDateColumn1(cellValue); }
                        case 16 -> candidate.setColumn2(cellValue);
                        case 17 -> candidate.setColumn3(cellValue);
                        case 18 -> candidate.setColumn4(cellValue);
                        case 19 -> candidate.setColumn5(cellValue);
                        case 20 -> candidate.setColumn6(cellValue);
                        case 21 -> candidate.setColumn7(cellValue);
                        case 22 -> candidate.setColumn8(cellValue);
                        case 23 -> candidate.setColumn9(cellValue);
                        case 24 -> candidate.setColumn10(cellValue);
                    }
                    columnCount++;
                }
                candidates.add(candidate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return candidates;
    }
}
