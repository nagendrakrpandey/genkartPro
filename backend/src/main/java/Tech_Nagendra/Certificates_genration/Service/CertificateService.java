package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Entity.TemplateImage;
import Tech_Nagendra.Certificates_genration.Repository.TemplateImageRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CertificateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateImageRepository templateImageRepository;

    public Map<String, Object> generateCertificatesAndReports(
            Long templateId,
            File excelFile,
            Map<String, File> uploadedFiles,
            String outputFolder,
            Long userId
    ) throws Exception {

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        validateUploadedFiles(template.getImageType(), excelFile, uploadedFiles);

        List<TemplateImage> templateImages = templateImageRepository.findByTemplateId(templateId);

        List<CandidateDTO> candidates = parseExcel(excelFile, template);

        Map<String, CandidateDTO> uniqueCandidates = new LinkedHashMap<>();
        for (CandidateDTO c : candidates) {
            uniqueCandidates.putIfAbsent(c.getSid(), c);
        }

        List<File> pdfFiles = new ArrayList<>();
        File jrxmlFile = new File(template.getJrxmlPath());
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());

        for (CandidateDTO candidate : uniqueCandidates.values()) {
            Map<String, Object> parameters = new HashMap<>();
            fillCandidateParameters(candidate, parameters);

            String templateFolder = "C:/certificate_storage/templates/" + template.getTemplateName();
            parameters.put("IMAGE_FOLDER", templateFolder);

            int imageType = template.getImageType();

            // Pass images as absolute paths (Strings) to avoid ClassCastException
            for (TemplateImage img : templateImages) {
                File f = new File(img.getImagePath());
                if (!f.exists()) {
                    System.out.println("Image not found: " + img.getImagePath());
                    continue;
                }

                String fileName = f.getName().toLowerCase();

                try {
                    if (fileName.contains("bg")) {
                        parameters.put("imgParamBG", f.getAbsolutePath());
                        continue;
                    }

                    switch (imageType) {
                        case 0: // all images dynamically
                            String paramName = fileName.split("\\.")[0]; // remove extension
                            parameters.put(paramName, f.getAbsolutePath());
                            break;
                        case 1: // only img4
                            if (fileName.contains("img4")) parameters.put("imgParam1", f.getAbsolutePath());
                            break;
                        case 2: // img1 and img2
                            if (fileName.contains("img1")) parameters.put("imgParam1", f.getAbsolutePath());
                            if (fileName.contains("img2")) parameters.put("imgParam2", f.getAbsolutePath());
                            break;
                        case 3: // img1, img2, img3
                            if (fileName.contains("img1")) parameters.put("imgParam1", f.getAbsolutePath());
                            if (fileName.contains("img2")) parameters.put("imgParam2", f.getAbsolutePath());
                            if (fileName.contains("img3")) parameters.put("imgParam3", f.getAbsolutePath());
                            break;
                    }

                    System.out.println("Added image to parameters: " + f.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Error loading image: " + img.getImagePath() + " -> " + e.getMessage());
                }
            }

            // Add uploaded files as absolute paths
            if (uploadedFiles != null) {
                if (imageType >= 1 && uploadedFiles.containsKey("zipImage"))
                    parameters.put("zipImage", uploadedFiles.get("zipImage").getAbsolutePath());
                if (imageType >= 2 && uploadedFiles.containsKey("logo"))
                    parameters.put("logo", uploadedFiles.get("logo").getAbsolutePath());
                if (imageType == 3 && uploadedFiles.containsKey("sign"))
                    parameters.put("sign", uploadedFiles.get("sign").getAbsolutePath());
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    new JRBeanCollectionDataSource(Collections.singleton(candidate))
            );

            String pdfName = candidate.getSid() + "_" + candidate.getCandidateName() + ".pdf";
            File pdfOut = new File(outputFolder, pdfName);
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfOut.getAbsolutePath());
            pdfFiles.add(pdfOut);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pdfFiles", pdfFiles);
        result.put("candidates", new ArrayList<>(uniqueCandidates.values()));
        return result;
    }

    private void validateUploadedFiles(int imageType, File excelFile, Map<String, File> uploadedFiles) {
        if (excelFile == null) throw new RuntimeException("Excel file is required");
        if (imageType >= 1 && (uploadedFiles == null || !uploadedFiles.containsKey("zipImage")))
            throw new RuntimeException("ZIP image is required for imageType " + imageType);
        if (imageType >= 2 && (uploadedFiles == null || !uploadedFiles.containsKey("logo")))
            throw new RuntimeException("Logo file is required for imageType " + imageType);
        if (imageType == 3 && (uploadedFiles == null || !uploadedFiles.containsKey("sign")))
            throw new RuntimeException("Signature file is required for imageType 3");
    }

    private void fillCandidateParameters(CandidateDTO c, Map<String, Object> params) {
        params.put("salutation", c.getSalutation());
        params.put("candidateName", c.getCandidateName());
        params.put("sid", c.getSid());
        params.put("jobrole", c.getJobrole());
        params.put("guardianType", c.getGuardianType());
        params.put("fatherORHusbandName", c.getFatherORHusbandName());
        params.put("sectorSkillCouncil", c.getSectorSkillCouncil());
        params.put("dateOfIssuance", formatDate(c.getDateOfIssuance()));
        params.put("level", c.getLevel());
        params.put("aadhaarNumber", c.getAadhaarNumber());
        params.put("sector", c.getSector());
        params.put("grade", c.getGrade());
        params.put("dateOfStart", formatDate(c.getDateOfStart()));
        params.put("dateOfEnd", formatDate(c.getDateOfEnd()));
        params.put("marks", c.getMarks());
        params.put("marks1", c.getMarks1());
        params.put("marks2", c.getMarks2());
        params.put("marks3", c.getMarks3());
        params.put("batchId", c.getBatchId());
    }

    private List<CandidateDTO> parseExcel(File excelFile, Template template) throws Exception {
        List<CandidateDTO> candidates = new ArrayList<>();
        if (excelFile == null) return candidates;

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                CandidateDTO dto = new CandidateDTO();
                dto.setSalutation(getCellValue(row.getCell(0)));
                dto.setCandidateName(getCellValue(row.getCell(1)));
                dto.setSid(getCellValue(row.getCell(2)));
                dto.setJobrole(getCellValue(row.getCell(3)));
                dto.setGuardianType(getCellValue(row.getCell(4)));
                dto.setFatherORHusbandName(getCellValue(row.getCell(5)));
                dto.setSectorSkillCouncil(getCellValue(row.getCell(6)));
                dto.setDateOfIssuance(getCellValue(row.getCell(7)));
                dto.setLevel(getCellValue(row.getCell(8)));
                dto.setAadhaarNumber(getCellValue(row.getCell(9)));
                dto.setSector(getCellValue(row.getCell(10)));
                dto.setGrade(getCellValue(row.getCell(11)));
                dto.setDateOfStart(getCellValue(row.getCell(12)));
                dto.setDateOfEnd(getCellValue(row.getCell(13)));
                dto.setMarks(getCellValue(row.getCell(14)));
                dto.setMarks1(getCellValue(row.getCell(15)));
                dto.setMarks2(getCellValue(row.getCell(16)));
                dto.setMarks3(getCellValue(row.getCell(17)));
                dto.setBatchId(getCellValue(row.getCell(18)));

                dto.setTemplate(template);
                candidates.add(dto);
            }
        }
        return candidates;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                return sdf.format(cell.getDateCellValue());
            }
            return String.valueOf((long) cell.getNumericCellValue());
        } else {
            return cell.toString().trim();
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            List<String> patterns = Arrays.asList("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy");
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat parser = new SimpleDateFormat(pattern);
                    Date date = parser.parse(dateStr);
                    return new SimpleDateFormat("dd-MM-yyyy").format(date);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return dateStr;
    }
}
