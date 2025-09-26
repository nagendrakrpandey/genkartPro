package Tech_Nagendra.Certificates_genration.Utility;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class Utility {
    
    public static List<CandidateDTO> getCandidatesFromFile(File file) throws Exception {
        List<CandidateDTO> candidates = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // First sheet
            int rowNum = 0;
            for (Row row : sheet) {
                // Skip header
                if (rowNum++ == 0) continue;

                CandidateDTO candidate = new CandidateDTO();

                candidate.setSalutation(getCellValue(row.getCell(0)));
                candidate.setCandidateName(getCellValue(row.getCell(1)));
                candidate.setSid(getCellValue(row.getCell(2)));
                candidate.setJobrole(getCellValue(row.getCell(3)));
                candidate.setGuardianType(getCellValue(row.getCell(4)));
                candidate.setFatherORHusbandName(getCellValue(row.getCell(5)));
                candidate.setSectorSkillCouncil(getCellValue(row.getCell(6)));
                candidate.setDateOfIssuance(getCellValue(row.getCell(7)));
                candidate.setNsqfLevel(getCellValue(row.getCell(8)));
                candidate.setAadhaarNumber(getCellValue(row.getCell(9)));
                candidate.setSector(getCellValue(row.getCell(10)));
                candidate.setGrade(getCellValue(row.getCell(11)));
                candidate.setDateOfStart(getCellValue(row.getCell(12)));
                candidate.setDateOfEnd(getCellValue(row.getCell(13)));
                candidate.setMarks(getCellValue(row.getCell(14)));
                candidate.setMarks1(getCellValue(row.getCell(15)));
                candidate.setMarks2(getCellValue(row.getCell(16)));
                candidate.setMarks3(getCellValue(row.getCell(17)));
                candidate.setBatchId(getCellValue(row.getCell(18)));

                // Add only valid candidates (sid required)
                if (candidate.getSid() != null && !candidate.getSid().isEmpty()) {
                    candidates.add(candidate);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error reading Excel file: " + e.getMessage());
        }

        return candidates;
    }

    // ---------------- Helper for reading cell ----------------
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                return String.valueOf((long) cell.getNumericCellValue());
            }
        }
        return "";
    }

    // ---------------- Placeholder Methods ----------------
    public static String convertObjectToXML(Object obj, Class<?> clazz) throws Exception {
        return "<Candidates></Candidates>";
    }

    public static String generateCertificateXML(String xml, int templateId) throws Exception {
        return System.getProperty("catalina.base") + "/files/output/XMLCertificate.pdf";
    }

    public static String generateImageEnabledCertificates(String xml, int templateId, CandidateDTO candidate, File logo, File sign) throws Exception {
        return System.getProperty("catalina.base") + "/files/output/XMLCertificate.pdf";
    }

    public static String splitandSavePDFs(String pdfPath, List<CandidateDTO> candidates, int templateId, int userId) throws Exception {
        return System.getProperty("catalina.base") + "/Certificates/";
    }

    public static String saveImagePDFs(String pdfPath, CandidateDTO candidate, int templateId, int userId) throws Exception {
        return System.getProperty("catalina.base") + "/Candidate Image Certificates/";
    }
}
