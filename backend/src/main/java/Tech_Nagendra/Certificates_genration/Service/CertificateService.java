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

    /**
     * Generates certificates in PDF for multiple candidates in a thread-safe way.
     *
     * @param templateId      Template ID
     * @param excelFile       Excel containing candidate data
     * @param uploadedFiles   Map of uploaded files (logo, sign, zipImage)
     * @param baseOutputFolder Base folder to store PDFs
     * @param userId          ID of the user generating certificates
     * @return Map with generated PDFs and folder path
     * @throws Exception
     */
    public Map<String, Object> generateCertificatesAndReports(
            Long templateId,
            File excelFile,
            Map<String, File> uploadedFiles,
            String baseOutputFolder,
            Long userId
    ) throws Exception {

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        validateUploadedFiles(template.getImageType(), excelFile, uploadedFiles);

        List<TemplateImage> templateImages = templateImageRepository.findByTemplateId(templateId);

        List<CandidateDTO> candidates = parseExcel(excelFile, template);

        // Remove duplicate candidates based on SID
        Map<String, CandidateDTO> uniqueCandidates = new LinkedHashMap<>();
        for (CandidateDTO c : candidates) {
            uniqueCandidates.putIfAbsent(c.getSid(), c);
        }

        List<File> pdfFiles = new ArrayList<>();
        File jrxmlFile = new File(template.getJrxmlPath());
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());

        // ✅ Unique folder per request to avoid collision
        String uniqueFolder = System.currentTimeMillis() + "_" + UUID.randomUUID();
        File outputFolder = new File(baseOutputFolder, uniqueFolder);
        if (!outputFolder.exists()) outputFolder.mkdirs();

        for (CandidateDTO candidate : uniqueCandidates.values()) {
            Map<String, Object> parameters = new HashMap<>();
            fillCandidateParameters(candidate, parameters);

            String templateFolder = "C:/certificate_storage/templates/" + template.getTemplateName();
            parameters.put("IMAGE_FOLDER", templateFolder);

            int imageType = template.getImageType();

            if (imageType == 0) {
                // Load all images from template folder
                File folder = new File(templateFolder);
                File[] files = folder.listFiles();
                if (files != null) {
                    int imgCount = 1;
                    for (File f : files) {
                        if (!f.isFile()) continue;
                        String fileName = f.getName().toLowerCase();
                        try {
                            if (fileName.contains("bg")) {
                                parameters.put("imgParamBG", f.getAbsolutePath());
                                continue;
                            }
                            parameters.put("imgParam" + imgCount, f.getAbsolutePath());
                            imgCount++;
                        } catch (Exception e) {
                            System.out.println("Error loading image: " + f.getAbsolutePath() + " -> " + e.getMessage());
                        }
                    }
                }
            } else {
                // Existing logic for imageType 1,2,3
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
                    } catch (Exception e) {
                        System.out.println("Error loading image: " + img.getImagePath() + " -> " + e.getMessage());
                    }
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

            // ✅ Filename as SID_CandidateName.pdf (no collision due to unique folder)
            String pdfName = candidate.getSid() + "_" + candidate.getCandidateName() + ".pdf";
            File pdfOut = new File(outputFolder, pdfName);
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfOut.getAbsolutePath());
            pdfFiles.add(pdfOut);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pdfFiles", pdfFiles);
        result.put("candidates", new ArrayList<>(uniqueCandidates.values()));
        result.put("folderPath", outputFolder.getAbsolutePath()); // optional for frontend download
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
        params.put("JobRole", c.getJobRole());
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
        params.put("marks4", c.getMarks4());
        params.put("marks5", c.getMarks5());
        params.put("marks6", c.getMarks6());
        params.put("marks7", c.getMarks7());
        params.put("marks8", c.getMarks8());
        params.put("marks9", c.getMarks9());
        params.put("marks10", c.getMarks10());
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
                dto.setJobRole(getCellValue(row.getCell(3)));
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
                dto.setMarks3(getCellValue(row.getCell(19)));
                dto.setMarks3(getCellValue(row.getCell(20)));
                dto.setMarks3(getCellValue(row.getCell(21)));
                dto.setMarks3(getCellValue(row.getCell(22)));
                dto.setMarks3(getCellValue(row.getCell(23)));
                dto.setMarks3(getCellValue(row.getCell(24)));
                dto.setMarks3(getCellValue(row.getCell(25)));
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
