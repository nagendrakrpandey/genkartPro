package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Repository.TemplateImageRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import Tech_Nagendra.Certificates_genration.Repository.ProfileRepository;
import Tech_Nagendra.Certificates_genration.Security.UserPrincipal;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CertificateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateImageRepository templateImageRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ReportService reportService;

    @Value("${file.upload-dir}")
    private String baseTemplateFolder;

    private static boolean fontsLoaded = false;

    private synchronized void loadAllFonts() {
        if (fontsLoaded) return;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            URL fontsDirURL = getClass().getResource("/fonts/");
            if (fontsDirURL != null) {
                File fontsDir = new File(fontsDirURL.toURI());
                File[] files = fontsDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".ttf") ||
                                name.toLowerCase().endsWith(".otf") ||
                                name.toLowerCase().endsWith(".jar")
                );
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().toLowerCase().endsWith(".jar")) {
                            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(f)) {
                                Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                                while (entries.hasMoreElements()) {
                                    java.util.jar.JarEntry entry = entries.nextElement();
                                    if (entry.getName().toLowerCase().endsWith(".ttf") ||
                                            entry.getName().toLowerCase().endsWith(".otf")) {
                                        try (InputStream is = jar.getInputStream(entry)) {
                                            java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
                                            ge.registerFont(font);
                                        }
                                    }
                                }
                            }
                        } else {
                            try (InputStream is = Files.newInputStream(f.toPath())) {
                                java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
                                ge.registerFont(font);
                            }
                        }
                    }
                }
            }
            fontsLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> generateCertificatesAndReports(
            Long templateId,
            File excelFile,
            Map<String, File> uploadedFiles,
            String outputFolderPath,
            UserPrincipal currentUser
    ) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        File outputFolder = new File(outputFolderPath);
        if (!outputFolder.exists()) outputFolder.mkdirs();

        return generateCertificatesByType(template, excelFile, uploadedFiles, outputFolder, currentUser);
    }

    public Map<String, Object> generateCertificatesByType(
            Template template,
            File excelFile,
            Map<String, File> uploadedFiles,
            File outputFolder,
            UserPrincipal currentUser
    ) throws Exception {
        int imageType = template.getImageType();
        switch (imageType) {
            case 1:
                return generateType1Certificates(template, excelFile, uploadedFiles, outputFolder, currentUser);
            case 2:
                return generateType2Certificates(template, excelFile, uploadedFiles, outputFolder, currentUser);
            case 3:
                return generateType3Certificates(template, excelFile, uploadedFiles, outputFolder, currentUser);
            default:
                return generateType0Certificates(template, excelFile, outputFolder, currentUser);
        }
    }

    private Map<String, Object> generateType0Certificates(
            Template template,
            File excelFile,
            File outputFolder,
            UserPrincipal currentUser
    ) throws Exception {
        return generateWithStaticImages(template, excelFile, null, outputFolder, 0, null, currentUser);
    }

    private Map<String, Object> generateType1Certificates(
            Template template,
            File excelFile,
            Map<String, File> uploadedFiles,
            File outputFolder,
            UserPrincipal currentUser
    ) throws Exception {
        File extractedZipFolder = extractZipImages(uploadedFiles, outputFolder);
        return generateWithStaticImages(template, excelFile, extractedZipFolder, outputFolder, 1, uploadedFiles, currentUser);
    }

    private Map<String, Object> generateType2Certificates(
            Template template,
            File excelFile,
            Map<String, File> uploadedFiles,
            File outputFolder,
            UserPrincipal currentUser
    ) throws Exception {
        File extractedZipFolder = extractZipImages(uploadedFiles, outputFolder);
        return generateWithStaticImages(template, excelFile, extractedZipFolder, outputFolder, 2, uploadedFiles, currentUser);
    }

    private Map<String, Object> generateType3Certificates(
            Template template,
            File excelFile,
            Map<String, File> uploadedFiles,
            File outputFolder,
            UserPrincipal currentUser
    ) throws Exception {
        File extractedZipFolder = extractZipImages(uploadedFiles, outputFolder);
        return generateWithStaticImages(template, excelFile, extractedZipFolder, outputFolder, 3, uploadedFiles, currentUser);
    }

    private File extractZipImages(Map<String, File> uploadedFiles, File outputFolder) throws IOException {
        if (uploadedFiles != null && uploadedFiles.containsKey("zipImage")) {
            File extractedZipFolder = new File(outputFolder, "unzippedImages");
            if (!extractedZipFolder.exists()) extractedZipFolder.mkdirs();
            unzipAndRenameImages(uploadedFiles.get("zipImage"), extractedZipFolder);
            return extractedZipFolder;
        }
        return null;
    }

    private Map<String, Object> generateWithStaticImages(
            Template template,
            File excelFile,
            File extractedZipFolder,
            File outputFolder,
            int imageType,
            Map<String, File> uploadedFiles,
            UserPrincipal currentUser
    ) throws Exception {
        List<File> pdfFiles = new ArrayList<>();
        Map<String, CandidateDTO> uniqueCandidates = new HashMap<>();
        List<File> templateStaticImages = loadStaticImages(template.getTemplateFolder());
        List<File> baseStaticImages = loadStaticImages(baseTemplateFolder);

        for (CandidateDTO candidate : parseExcel(excelFile, template)) {
            uniqueCandidates.put(candidate.getSid(), candidate);

            Report report = new Report();
            report.setSid(candidate.getSid());
            report.setCourseName(candidate.getCandidateName());
            report.setGrade(candidate.getGrade());
            report.setBatchId(candidate.getBatchId());
            report.setTemplateName(candidate.getTemplate() != null ? candidate.getTemplate().getTemplateName() : null);
            report.setJobrole(candidate.getJobRole());
            report.setLevel(candidate.getLevel());
            report.setTemplate(candidate.getTemplate());

            reportService.saveOrUpdateBySid(report, currentUser);

            JasperReport jasperReport = JasperCompileManager.compileReport(template.getJrxmlPath());
            Map<String, Object> parameters = new HashMap<>();
            List<File> allStaticImages = new ArrayList<>();
            allStaticImages.addAll(templateStaticImages);
            allStaticImages.addAll(baseStaticImages);

            File bgImage = allStaticImages.stream()
                    .filter(f -> f.getName().toLowerCase().contains("bg"))
                    .findFirst().orElse(null);
            if (bgImage != null) parameters.put("imgParamBG", bgImage.getAbsolutePath());

            int imgIndex = 1;
            for (File f : allStaticImages) {
                if (bgImage != null && f.equals(bgImage)) continue;
                parameters.put("imgParam" + imgIndex++, f.getAbsolutePath());
            }

            if (imageType >= 1 && extractedZipFolder != null) {
                File candidateImg = findCandidateImage(extractedZipFolder, candidate.getSid());
                if (candidateImg != null) parameters.put("imgParam3", candidateImg.getAbsolutePath());
            }
            if (imageType >= 2 && uploadedFiles != null && uploadedFiles.containsKey("logo")) {
                parameters.put("imgParam5", uploadedFiles.get("logo").getAbsolutePath());
            }
            if (imageType >= 3 && uploadedFiles != null && uploadedFiles.containsKey("sign")) {
                parameters.put("imgParam6", uploadedFiles.get("sign").getAbsolutePath());
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
        result.put("folderPath", outputFolder.getAbsolutePath());
        return result;
    }

    private List<File> loadStaticImages(String folderPath) {
        List<File> images = new ArrayList<>();
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && isImageFile(f.getName())) images.add(f);
                }
            }
        }
        return images;
    }

    private void unzipAndRenameImages(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = new File(entry.getName()).getName();
                String extension = entryName.substring(entryName.lastIndexOf('.')).toLowerCase();
                String sidName = entryName.substring(0, entryName.lastIndexOf('.'));
                File newFile = new File(destDir, sidName + extension);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private File findCandidateImage(File folder, String sid) {
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().toLowerCase().contains(sid.toLowerCase()) && isImageFile(f.getName()))
                return f;
        }
        return null;
    }

    private boolean isImageFile(String name) {
        return name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png") ;
    }

    private List<CandidateDTO> parseExcel(File excelFile, Template template) throws Exception {
        List<CandidateDTO> candidates = new ArrayList<>();
        if (excelFile == null) return candidates;
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                if (isRowEmpty(row)) continue;

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
                dto.setMarks4(getCellValue(row.getCell(18)));
                dto.setMarks5(getCellValue(row.getCell(19)));
                dto.setMarks6(getCellValue(row.getCell(20)));
                dto.setMarks7(getCellValue(row.getCell(21)));
                dto.setMarks8(getCellValue(row.getCell(22)));
                dto.setMarks9(getCellValue(row.getCell(23)));
                dto.setMarks10(getCellValue(row.getCell(24)));
                dto.setBatchId(getCellValue(row.getCell(25)));
                dto.setState(getCellValue(row.getCell(26)));
                dto.setDistrict(getCellValue(row.getCell(27)));
                dto.setPlace(getCellValue(row.getCell(28)));
                dto.setTemplate(template);

                if (isValidCandidate(dto)) {
                    candidates.add(dto);
                }
            }
        }
        return candidates;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidCandidate(CandidateDTO candidate) {
        return candidate.getSid() != null && !candidate.getSid().trim().isEmpty() &&
                candidate.getCandidateName() != null && !candidate.getCandidateName().trim().isEmpty();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                        return sdf.format(cell.getDateCellValue());
                    }

                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:

                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);

                        switch (cellValue.getCellType()) {
                            case STRING:
                                return cellValue.getStringValue().trim();
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                                    return sdf.format(cellValue.getNumberValue());
                                }
                                double formulaNumValue = cellValue.getNumberValue();
                                if (formulaNumValue == Math.floor(formulaNumValue)) {
                                    return String.valueOf((long) formulaNumValue);
                                } else {
                                    return String.valueOf(formulaNumValue);
                                }
                            case BOOLEAN:
                                return String.valueOf(cellValue.getBooleanValue());
                            default:
                                return "";
                        }
                    } catch (Exception e) {

                        return cell.getCellFormula();
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
    }
