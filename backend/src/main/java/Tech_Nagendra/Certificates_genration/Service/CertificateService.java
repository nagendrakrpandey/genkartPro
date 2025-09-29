package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Entity.TemplateImage;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateImageRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CertificateService {

    private final TemplateRepository templateRepository;
    private final TemplateImageRepository templateImageRepository;

    public CertificateService(TemplateRepository templateRepository, TemplateImageRepository templateImageRepository) {
        this.templateRepository = templateRepository;
        this.templateImageRepository = templateImageRepository;
    }

    /**
     * Generate individual PDF certificates and save them in tempDir
     */
    public List<File> generateCertificatesToLocal(Long templateId, File excelFile, String tempDir) throws Exception {

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        List<TemplateImage> templateImages = templateImageRepository.findByTemplateId(templateId);

        File jrxmlFile = new File(template.getJrxmlPath());
        if (!jrxmlFile.exists()) throw new FileNotFoundException("JRXML file not found");

        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());

        List<CandidateDTO> candidates = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // skip header

                CandidateDTO c = new CandidateDTO();
                c.setSalutation(getCellString(row.getCell(0)));
                c.setCandidateName(getCellString(row.getCell(1)));
                c.setSid(getCellString(row.getCell(2)));
                c.setJobrole(getCellString(row.getCell(3)));
                c.setGuardianType(getCellString(row.getCell(4)));
                c.setFatherORHusbandName(getCellString(row.getCell(5)));
                c.setSectorSkillCouncil(getCellString(row.getCell(6)));
                c.setDateOfIssuance(formatDateCell(row.getCell(7)));
                c.setLevel(getCellString(row.getCell(8)));
                c.setAadhaarNumber(getCellString(row.getCell(9)));
                c.setSector(getCellString(row.getCell(10)));
                c.setGrade(getCellString(row.getCell(11)));
                c.setDateOfStart(formatDateCell(row.getCell(12)));
                c.setDateOfEnd(formatDateCell(row.getCell(13)));
                c.setMarks(getCellString(row.getCell(14)));
                c.setMarks1(getCellString(row.getCell(15)));
                c.setMarks2(getCellString(row.getCell(16)));
                c.setMarks3(getCellString(row.getCell(17)));
                c.setBatchId(getCellString(row.getCell(18)));
                c.setTemplate(template);

                candidates.add(c);
            }
        }

        // Ensure tempDir exists
        File dir = new File(tempDir);
        if (!dir.exists()) dir.mkdirs();

        List<File> pdfFiles = new ArrayList<>();

        // Generate PDF for each candidate
        for (CandidateDTO candidate : candidates) {
            Map<String, Object> parameters = new HashMap<>();
            int imgCounter = 1;

            for (TemplateImage img : templateImages) {
                File imgFile = new File(img.getImagePath());
                if (!imgFile.exists()) continue;

                // If filename contains BG -> background image
                if (img.getImagePath().toLowerCase().contains("bg")) {
                    parameters.put("imgParamBG", imgFile.getAbsolutePath());
                } else {
                    parameters.put("imgParam" + imgCounter, imgFile.getAbsolutePath());
                    imgCounter++;
                }
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(candidate));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            File pdfFile = new File(dir, candidate.getCandidateName().replaceAll("\\s+", "_") + ".pdf");
            JasperExportManager.exportReportToPdfStream(jasperPrint, new FileOutputStream(pdfFile));
            pdfFiles.add(pdfFile);
        }

        return pdfFiles;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return new SimpleDateFormat("dd-MM-yy").format(cell.getDateCellValue());
                else return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    private String formatDateCell(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new SimpleDateFormat("dd-MM-yy").format(cell.getDateCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        return "";
    }
}
