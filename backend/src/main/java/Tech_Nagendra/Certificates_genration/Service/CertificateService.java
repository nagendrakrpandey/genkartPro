package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Repository.TemplateImageRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CertificateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateImageRepository templateImageRepository;

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
                                            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                                            ge.registerFont(font);
                                        }
                                    }
                                }
                            }
                        } else {
                            try (InputStream is = Files.newInputStream(f.toPath())) {
                                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
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

    private boolean isValidImage(File file) {
        try {
            return file != null && file.exists() && ImageIO.read(file) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> generateCertificatesAndReports(
            Long templateId,
            File excelFile,
            Map<String, File> uploadedFiles,
            String outputFolderPath,
            Long userId
    ) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        File outputFolder = new File(outputFolderPath);
        if (!outputFolder.exists()) outputFolder.mkdirs();
        return generateCertificates(template, excelFile, uploadedFiles, outputFolder);
    }

    private Map<String, Object> generateCertificates(
            Template template,
            File excelFile,
            Map<String, File> uploadedFiles,
            File outputFolder
    ) throws Exception {

        int imageType = template.getImageType();
        String templateFolder = template.getTemplateFolder();
        List<File> pdfFiles = new ArrayList<>();
        Map<String, CandidateDTO> uniqueCandidates = new HashMap<>();

        validateUploadedFiles(imageType, excelFile, uploadedFiles);

        File extractedZipFolder = null;
        if (imageType >= 1 && uploadedFiles != null && uploadedFiles.containsKey("zipImage")) {
            extractedZipFolder = new File(outputFolder, "unzippedImages");
            if (!extractedZipFolder.exists()) extractedZipFolder.mkdirs();
            unzipAndRenameImages(uploadedFiles.get("zipImage"), extractedZipFolder);
        }

        List<File> staticImages = loadStaticImages(templateFolder);

        for (CandidateDTO candidate : parseExcel(excelFile, template)) {
            uniqueCandidates.put(candidate.getSid(), candidate);
            JasperReport jasperReport = JasperCompileManager.compileReport(template.getJrxmlPath());
            Map<String, Object> parameters = new HashMap<>();

            int imgIndex = 1;
            for (File img : staticImages) {
                String name = img.getName().toLowerCase();
                if (name.contains("bg")) {
                    parameters.put("imgParamBG", img.getAbsolutePath());
                } else {
                    parameters.put("imgParam" + imgIndex++, img.getAbsolutePath());
                }
            }

            if (imageType >= 1 && extractedZipFolder != null) {
                File candidateImg = findCandidateImage(extractedZipFolder, candidate.getSid());
                if (candidateImg != null && candidateImg.exists()) {
                    parameters.put("imgParam4", candidateImg.getAbsolutePath());
                }
            }

            if (imageType >= 2 && uploadedFiles != null && uploadedFiles.containsKey("logo")) {
                parameters.put("imgParam5", uploadedFiles.get("logo").getAbsolutePath());
            }

            if (imageType == 3 && uploadedFiles != null && uploadedFiles.containsKey("sign")) {
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
            for (File f : Objects.requireNonNull(folder.listFiles())) {
                if (f.isFile() && isImageFile(f.getName())) images.add(f);
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
                String sidName = entryName.replaceAll("[^0-9]", "");
                if (sidName.isEmpty()) sidName = UUID.randomUUID().toString();
                File newFile = new File(destDir, sidName + extension);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    zis.transferTo(fos);
                }
            }
        }
    }

    private File findCandidateImage(File folder, String sid) {
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (name.contains(sid.toLowerCase()) && isImageFile(name)) return f;
        }
        return null;
    }

    private boolean isImageFile(String name) {
        return name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png");
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
        List<String> patterns = Arrays.asList("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy");
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern);
                Date date = parser.parse(dateStr);
                return new SimpleDateFormat("dd-MM-yyyy").format(date);
            } catch (Exception ignored) {}
        }
        return dateStr;
    }
}
