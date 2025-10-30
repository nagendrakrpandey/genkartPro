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
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.type.PdfaConformanceEnum;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.Font;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateImageRepository templateImageRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ReportService reportService;

   // @Value("${file.upload-dir:C:/certificate_storage/templates/}")
    @Value("${certificate.template.path:${user.dir}/templates/}")
    private String baseTemplateFolder;

    @Value("${custom.fonts.lib:lib}")
    private String libsFolder;

    @Value("${custom.fonts.dir:src/main/resources/fonts}")
    private String classpathFontsDir;

    private static volatile boolean fontsLoaded = false;

    private synchronized void loadAllFonts() {
        if (fontsLoaded) return;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            // 1) Load from classpath resources (/src/main/resources/fonts)
            try {
                URL fontsDirUrl = getClass().getResource("/fonts/");
                if (fontsDirUrl != null) {
                    File fontsDir;
                    try {
                        fontsDir = new File(fontsDirUrl.toURI());
                    } catch (URISyntaxException e) {
                        fontsDir = new File(fontsDirUrl.getPath());
                    }
                    loadFontsFromFolder(fontsDir);
                } else {
                    loadFontsFromClasspathFolder("/fonts/");
                }
            } catch (Exception e) {
                logger.info("Classpath fonts scan error: {}", e.getMessage());
            }

            // 2) Load from configured upload-dir
            try {
                File externalFonts = new File(baseTemplateFolder);
                if (externalFonts.exists() && externalFonts.isDirectory()) {
                    loadFontsFromFolder(externalFonts);
                } else {
                    // Also try fonts subfolder
                    File fontsSub = new File(baseTemplateFolder, "fonts");
                    if (fontsSub.exists() && fontsSub.isDirectory()) loadFontsFromFolder(fontsSub);
                }
            } catch (Exception e) {
                logger.info("External fonts scan error: {}", e.getMessage());
            }

            // 3) Load font jars from libs folder
            try {
                File libDir = new File(libsFolder);
                if (libDir.exists() && libDir.isDirectory()) {
                    File[] jars = libDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
                    if (jars != null) {
                        for (File j : jars) {
                            loadFontsFromJarFile(j);
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("Libs folder scan error: {}", e.getMessage());
            }


            try {
                loadSystemFonts();
            } catch (Exception ignored) {}

            // 5) Set JasperReports properties for HTML markup & font embedding
            setupJasperReportsProperties();

            fontsLoaded = true;
            logger.info("Fonts loaded and JasperReports properties set.");
        } catch (Exception e) {
            logger.error("Error loading fonts", e);
            try { setupJasperReportsProperties(); } catch (Exception ignore) {}
            fontsLoaded = true;
        }
    }

    private void loadFontsFromFolder(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            try {
                if (f.isDirectory()) {
                    loadFontsFromFolder(f);
                } else {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".ttf") || name.endsWith(".otf")) {
                        try (InputStream is = Files.newInputStream(f.toPath())) {
                            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                            logger.info("Registered font file: {}", f.getAbsolutePath());
                        } catch (Exception ex) {
                            logger.warn("Failed to register font file {} : {}", f.getName(), ex.getMessage());
                        }
                    } else if (name.endsWith(".jar")) {
                        loadFontsFromJarFile(f);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error processing {} : {}", f.getName(), e.getMessage());
            }
        }
    }

    private void loadFontsFromClasspathFolder(String resourceFolder) {
        try {
            URL url = getClass().getResource(resourceFolder);
            if (url == null) return;
            String protocol = url.getProtocol();
            if ("jar".equals(protocol)) {
                String path = url.getPath();
                String jarPath;
                if (path.startsWith("file:")) {
                    jarPath = path.substring(5, path.indexOf("!"));
                } else {
                    int excl = path.indexOf("!");
                    jarPath = path.substring(0, excl);
                }
                try (JarInputStream jis = new JarInputStream(new FileInputStream(jarPath))) {
                    JarEntry entry;
                    while ((entry = jis.getNextJarEntry()) != null) {
                        String name = entry.getName().toLowerCase();
                        if (name.startsWith("fonts/") && (name.endsWith(".ttf") || name.endsWith(".otf"))) {
                            try (InputStream is = getClass().getResourceAsStream("/" + entry.getName())) {
                                if (is != null) {
                                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                                    logger.info("Registered classpath font: {}", entry.getName());
                                }
                            } catch (Exception ex) {
                                logger.warn("Failed to register classpath font {} : {}", entry.getName(), ex.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed scanning jar for fonts: {}", e.getMessage());
                }
            } else if ("file".equals(protocol)) {
                try {
                    File folder = new File(url.toURI());
                    loadFontsFromFolder(folder);
                } catch (URISyntaxException e) {
                    logger.warn("Invalid URI for classpath fonts folder: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load fonts from classpath folder {} : {}", resourceFolder, e.getMessage());
        }
    }

    private void loadFontsFromJarFile(File jarFile) {
        if (jarFile == null || !jarFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(jarFile);
             JarInputStream jis = new JarInputStream(fis)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".ttf") || name.endsWith(".otf")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = jis.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    try (InputStream is = new ByteArrayInputStream(baos.toByteArray())) {
                        Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                        logger.info("Registered font from jar {} -> {}", jarFile.getName(), name);
                    } catch (Exception ex) {
                        logger.warn("Failed to create font from jar entry {} : {}", name, ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed reading jar {} : {}", jarFile.getName(), ex.getMessage());
        }
    }

    private void loadSystemFonts() {
        String[] fontDirs = {
                "C:\\Windows\\Fonts",
                "/usr/share/fonts",
                "/usr/local/share/fonts",
                "/Library/Fonts"
        };
        for (String fd : fontDirs) {
            try {
                File dir = new File(fd);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".ttf") || n.toLowerCase().endsWith(".otf"));
                    if (files != null) {
                        for (File f : files) {
                            try (InputStream is = Files.newInputStream(f.toPath())) {
                                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupJasperReportsProperties() {
        JRPropertiesUtil props = JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance());
        props.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
        props.setProperty("net.sf.jasperreports.print.keep.full.text", "true");
        props.setProperty("net.sf.jasperreports.export.pdf.force.linebreak.policy", "true");
        props.setProperty("net.sf.jasperreports.text.truncate.at.char", "false");
        props.setProperty("net.sf.jasperreports.text.truncate.suffix", "");
        props.setProperty("net.sf.jasperreports.export.pdf.embedded", "true");
        props.setProperty("net.sf.jasperreports.export.pdf.font.embedded", "true");
        props.setProperty("net.sf.jasperreports.text.markup.html", "html");
        props.setProperty("net.sf.jasperreports.markup.processor.factory", "net.sf.jasperreports.engine.util.HtmlMarkupProcessorFactory");
        props.setProperty("net.sf.jasperreports.markup.parser.html.enabled", "true");
    }

    public Map<String, Object> generateCertificatesAndReports(
            Long templateId,
            File excelFile,
            Map<String, File> uploadedFiles,
            String outputFolderPath,
            UserPrincipal currentUser
    ) {
        try {
            Template template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));
            File outputFolder = new File(outputFolderPath);
            if (!outputFolder.exists() && !outputFolder.mkdirs()) throw new RuntimeException("Failed to create output dir");
            return generateCertificatesByType(template, excelFile, uploadedFiles, outputFolder, currentUser);
        } catch (Exception e) {
            logger.error("Generation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            error.put("pdfFiles", new ArrayList<File>());
            error.put("candidates", new ArrayList<CandidateDTO>());
            error.put("folderPath", "");
            return error;
        }
    }

    public Map<String, Object> generateCertificatesByType(
            Template template,
            File excelFile,
            Map<String, File> uploadedFiles,
            File outputFolder,
            UserPrincipal currentUser
    ) throws Exception {
        loadAllFonts();
        int imageType = template.getImageType();
        switch (imageType) {
            case 1: return generateType1Certificates(template, excelFile, uploadedFiles, outputFolder, currentUser);
            case 2: return generateType2Certificates(template, excelFile, uploadedFiles, outputFolder, currentUser);
            case 3: return generateType3Certificates(template, excelFile, uploadedFiles, outputFolder, currentUser);
            default: return generateType0Certificates(template, excelFile, outputFolder, currentUser);
        }
    }

    private Map<String, Object> generateType0Certificates(Template template, File excelFile, File outputFolder, UserPrincipal currentUser) throws Exception {
        return generateWithStaticImages(template, excelFile, null, outputFolder, 0, null, currentUser);
    }

    private Map<String, Object> generateType1Certificates(Template template, File excelFile, Map<String, File> uploadedFiles, File outputFolder, UserPrincipal currentUser) throws Exception {
        File extracted = extractZipImages(uploadedFiles, outputFolder);
        return generateWithStaticImages(template, excelFile, extracted, outputFolder, 1, uploadedFiles, currentUser);
    }

    private Map<String, Object> generateType2Certificates(Template template, File excelFile, Map<String, File> uploadedFiles, File outputFolder, UserPrincipal currentUser) throws Exception {
        File extracted = extractZipImages(uploadedFiles, outputFolder);
        return generateWithStaticImages(template, excelFile, extracted, outputFolder, 2, uploadedFiles, currentUser);
    }

    private Map<String, Object> generateType3Certificates(Template template, File excelFile, Map<String, File> uploadedFiles, File outputFolder, UserPrincipal currentUser) throws Exception {
        File extracted = extractZipImages(uploadedFiles, outputFolder);
        return generateWithStaticImages(template, excelFile, extracted, outputFolder, 3, uploadedFiles, currentUser);
    }

    private File extractZipImages(Map<String, File> uploadedFiles, File outputFolder) throws IOException {
        if (uploadedFiles != null && uploadedFiles.containsKey("zipImage")) {
            File extracted = new File(outputFolder, "unzippedImages");
            if (!extracted.exists()) extracted.mkdirs();
            unzipAndRenameImages(uploadedFiles.get("zipImage"), extracted);
            return extracted;
        }
        return null;
    }

    private Map<String, Object> generateWithStaticImages(Template template, File excelFile, File extractedZipFolder, File outputFolder, int imageType, Map<String, File> uploadedFiles, UserPrincipal currentUser) throws Exception {
        List<File> pdfFiles = new ArrayList<>();
        Map<String, CandidateDTO> uniqueBySid = new LinkedHashMap<>();
        Map<String, Integer> sidIndexMap = new HashMap<>();
        List<CandidateDTO> candidates = parseExcel(excelFile, template);
        if (candidates == null || candidates.isEmpty()) throw new Exception("No candidates found");
        List<File> templateStaticImages = loadStaticImages(template.getTemplateFolder());
        List<File> baseStaticImages = loadStaticImages(baseTemplateFolder);
        for (CandidateDTO candidate : candidates) {
            String sid = candidate.getSid();
            if (sid == null || sid.trim().isEmpty()) continue;
            if (!uniqueBySid.containsKey(sid)) {
                uniqueBySid.put(sid, candidate);
                Report report = createReport(candidate, currentUser);
                reportService.saveOrUpdateBySid(report, currentUser);
                File pdfFile = generateCertificateForCandidate(template, candidate, templateStaticImages, baseStaticImages, extractedZipFolder, imageType, uploadedFiles, outputFolder);
                sidIndexMap.put(sid, pdfFiles.size());
                pdfFiles.add(pdfFile);
            } else {
                uniqueBySid.put(sid, candidate);
                Report report = createReport(candidate, currentUser);
                reportService.saveOrUpdateBySid(report, currentUser);
                Integer idx = sidIndexMap.get(sid);
                if (idx != null) {
                    File old = pdfFiles.get(idx);
                    if (old.exists()) old.delete();
                    File pdfFile = generateCertificateForCandidate(template, candidate, templateStaticImages, baseStaticImages, extractedZipFolder, imageType, uploadedFiles, outputFolder);
                    pdfFiles.set(idx, pdfFile);
                } else {
                    File pdfFile = generateCertificateForCandidate(template, candidate, templateStaticImages, baseStaticImages, extractedZipFolder, imageType, uploadedFiles, outputFolder);
                    sidIndexMap.put(sid, pdfFiles.size());
                    pdfFiles.add(pdfFile);
                }
            }
        }
        return createResultMap(pdfFiles, uniqueBySid, outputFolder);
    }

    private Report createReport(CandidateDTO candidate, UserPrincipal currentUser) {
        Report report = new Report();
        report.setSid(candidate.getSid());
        report.setCandidateName(candidate.getCandidateName());
        report.setGrade(candidate.getGrade());
        report.setBatchId(candidate.getBatchId());
        report.setTemplateName(candidate.getTemplate() != null ? candidate.getTemplate().getTemplateName() : null);
        report.setJobrole(candidate.getJobRole());
        report.setLevel(candidate.getLevel());
        report.setTemplate(candidate.getTemplate());
        return report;
    }

    private File generateCertificateForCandidate(Template template, CandidateDTO candidate, List<File> templateStaticImages, List<File> baseStaticImages, File extractedZipFolder, int imageType, Map<String, File> uploadedFiles, File outputFolder) throws Exception {
        if (template.getJrxmlPath() == null || template.getJrxmlPath().trim().isEmpty()) throw new IllegalArgumentException("JRXML path missing");
        JasperReport jasperReport = JasperCompileManager.compileReport(template.getJrxmlPath());
        Map<String, Object> parameters = createJasperParameters();
        setupImageParameters(parameters, templateStaticImages, baseStaticImages, extractedZipFolder, imageType, uploadedFiles, candidate);
        CandidateDTO dataCandidate = createModifiedCandidateForHtml(candidate);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(Collections.singletonList(dataCandidate)));
        return exportToPdf(jasperPrint, candidate, outputFolder);
    }

    private CandidateDTO createModifiedCandidateForHtml(CandidateDTO original) {
        CandidateDTO m = new CandidateDTO();
        m.setSalutation(original.getSalutation());
        m.setCandidateName(original.getCandidateName());
        m.setSid(original.getSid());
        m.setJobRole(original.getJobRole());
        m.setGuardianType(original.getGuardianType());
        m.setFatherORHusbandName(original.getFatherORHusbandName());
        m.setSectorSkillCouncil(original.getSectorSkillCouncil());
        m.setDateOfIssuance(original.getDateOfIssuance());
        m.setLevel(original.getLevel());
        m.setAadhaarNumber(original.getAadhaarNumber());
        m.setSector(original.getSector());
        m.setGrade(original.getGrade());
        m.setDateOfStart(original.getDateOfStart());
        m.setDateOfEnd(original.getDateOfEnd());
        m.setMarks(original.getMarks());
        m.setMarks1(original.getMarks1());
        m.setMarks2(original.getMarks2());
        m.setMarks3(original.getMarks3());
        m.setMarks4(original.getMarks4());
        m.setMarks5(original.getMarks5());
        m.setMarks6(original.getMarks6());
        m.setMarks7(original.getMarks7());
        m.setMarks8(original.getMarks8());
        m.setMarks9(original.getMarks9());
        m.setMarks10(original.getMarks10());
        m.setBatchId(original.getBatchId());
        m.setState(original.getState());
        m.setDistrict(original.getDistrict());
        m.setCourseName(original.getCourseName());
        m.setCandidateName(original.getDuration());
        m.setPlace(original.getPlace());
        m.setTemplate(original.getTemplate());
        return m;
    }

    private Map<String, Object> createJasperParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.REPORT_LOCALE, Locale.ENGLISH);
        parameters.put("net.sf.jasperreports.default.font.name", "Helvetica");
        parameters.put("net.sf.jasperreports.export.pdf.font.embedded", true);
        parameters.put("net.sf.jasperreports.print.keep.full.text", true);
        parameters.put("net.sf.jasperreports.export.pdf.force.linebreak.policy", true);
        parameters.put("net.sf.jasperreports.text.truncate.at.char", false);
        parameters.put("net.sf.jasperreports.text.truncate.suffix", "");
        parameters.put("net.sf.jasperreports.text.markup.html", "html");
        parameters.put("net.sf.jasperreports.markup.processor.factory", "net.sf.jasperreports.engine.util.HtmlMarkupProcessorFactory");
        parameters.put("net.sf.jasperreports.markup.parser.html.enabled", true);
        parameters.put("net.sf.jasperreports.awt.ignore.missing.font", "true");
        return parameters;
    }

    private void setupImageParameters(Map<String, Object> parameters, List<File> templateStaticImages, List<File> baseStaticImages, File extractedZipFolder, int imageType, Map<String, File> uploadedFiles, CandidateDTO candidate) {
        List<File> all = new ArrayList<>();
        if (templateStaticImages != null) all.addAll(templateStaticImages);
        if (baseStaticImages != null) all.addAll(baseStaticImages);
        File bg = all.stream().filter(f -> f.getName().toLowerCase().contains("bg")).findFirst().orElse(null);
        if (bg != null) parameters.put("imgParamBG", bg.getAbsolutePath());
        int idx = 1;
        for (File f : all) {
            if (bg != null && f.equals(bg)) continue;
            parameters.put("imgParam" + idx++, f.getAbsolutePath());
            if (idx > 15) break;
        }
        if (imageType >= 1 && extractedZipFolder != null) {
            File candidateImg = findCandidateImage(extractedZipFolder, candidate.getSid());
            if (candidateImg != null) parameters.put("imgParam3", candidateImg.getAbsolutePath());
        }
        if (imageType >= 2 && uploadedFiles != null && uploadedFiles.containsKey("logo")) parameters.put("imgParam5", uploadedFiles.get("logo").getAbsolutePath());
        if (imageType >= 3 && uploadedFiles != null && uploadedFiles.containsKey("signature")) parameters.put("imgParam6", uploadedFiles.get("signature").getAbsolutePath());
    }

    private File exportToPdf(JasperPrint jasperPrint, CandidateDTO candidate, File outputFolder) throws JRException {
        String safeName = candidate.getCandidateName() == null ? "unknown" : candidate.getCandidateName().replaceAll("[^a-zA-Z0-9\\-_]", "_");
        String sid = candidate.getSid() == null ? String.valueOf(System.currentTimeMillis()) : candidate.getSid().replaceAll("[^a-zA-Z0-9\\-_]", "_");
        String pdfName = sid + "_" + safeName + ".pdf";
        File out = new File(outputFolder, pdfName);
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        reportConfig.setForceLineBreakPolicy(true);
        reportConfig.setForceSvgShapes(true);
        SimplePdfExporterConfiguration exportConfig = new SimplePdfExporterConfiguration();
        exportConfig.setPdfaConformance(PdfaConformanceEnum.NONE);
        exportConfig.setMetadataAuthor("Certificate Generator");
        exportConfig.setTagged(true);
        exporter.setConfiguration(reportConfig);
        exporter.setConfiguration(exportConfig);
        exporter.exportReport();
        return out;
    }

    private Map<String, Object> createResultMap(List<File> pdfFiles, Map<String, CandidateDTO> uniqueBySid, File outputFolder) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", false);
        result.put("message", "Certificates generated");
        result.put("pdfFiles", pdfFiles);
        result.put("candidates", new ArrayList<>(uniqueBySid.values()));
        result.put("folderPath", outputFolder.getAbsolutePath());
        result.put("totalGenerated", pdfFiles.size());
        return result;
    }

    private List<File> loadStaticImages(String folderPath) {
        List<File> images = new ArrayList<>();
        if (folderPath == null) return images;
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) return images;
        File[] files = folder.listFiles();
        if (files == null) return images;
        for (File f : files) if (f.isFile() && isImageFile(f.getName())) images.add(f);
        return images;
    }

    private void unzipAndRenameImages(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = new File(entry.getName()).getName();
                int dot = entryName.lastIndexOf('.');
                String ext = dot > 0 ? entryName.substring(dot).toLowerCase() : "";
                String name = dot > 0 ? entryName.substring(0, dot) : entryName;
                File newFile = new File(destDir, name + ext);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buf = new byte[1024]; int len;
                    while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                }
                zis.closeEntry();
            }
        }
    }

    private File findCandidateImage(File folder, String sid) {
        if (folder == null || sid == null) return null;
        File[] files = folder.listFiles(); if (files == null) return null;
        for (File f : files) {
            if (f.getName().toLowerCase().contains(sid.toLowerCase()) && isImageFile(f.getName())) return f;
        }
        return null;
    }

    private boolean isImageFile(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif") || n.endsWith(".bmp");
    }

    private List<CandidateDTO> parseExcel(File excelFile, Template template) throws Exception {
        List<CandidateDTO> candidates = new ArrayList<>();
        if (excelFile == null || !excelFile.exists()) throw new FileNotFoundException("Excel file missing");
        try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0); if (sheet == null) throw new Exception("No sheet");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i); if (row == null || isRowEmpty(row)) continue;
                CandidateDTO c = createCandidateFromRow(row, template);
                if (isValidCandidate(c)) candidates.add(c);
            }
        }
        return candidates;
    }

    private CandidateDTO createCandidateFromRow(Row row, Template template) {
        CandidateDTO candidate = new CandidateDTO();
        candidate.setSalutation(getSafeCellValue(row.getCell(0)));
        candidate.setCandidateName(getSafeCellValue(row.getCell(1)));
        candidate.setSid(getSafeCellValue(row.getCell(2)));
        candidate.setJobRole(getSafeCellValue(row.getCell(3)));
        candidate.setGuardianType(getSafeCellValue(row.getCell(4)));
        candidate.setFatherORHusbandName(getSafeCellValue(row.getCell(5)));
        candidate.setSectorSkillCouncil(getSafeCellValue(row.getCell(6)));
        candidate.setDateOfIssuance(getSafeCellValue(row.getCell(7)));
        candidate.setLevel(getSafeCellValue(row.getCell(8)));
        candidate.setAadhaarNumber(getSafeCellValue(row.getCell(9)));
        candidate.setSector(getSafeCellValue(row.getCell(10)));
        candidate.setGrade(getSafeCellValue(row.getCell(11)));
        candidate.setDateOfStart(getSafeCellValue(row.getCell(12)));
        candidate.setDateOfEnd(getSafeCellValue(row.getCell(13)));
        candidate.setMarks(getSafeCellValue(row.getCell(14)));
        candidate.setMarks1(getSafeCellValue(row.getCell(15)));
        candidate.setMarks2(getSafeCellValue(row.getCell(16)));
        candidate.setMarks3(getSafeCellValue(row.getCell(17)));
        candidate.setMarks4(getSafeCellValue(row.getCell(18)));
        candidate.setMarks5(getSafeCellValue(row.getCell(19)));
        candidate.setMarks6(getSafeCellValue(row.getCell(20)));
        candidate.setMarks7(getSafeCellValue(row.getCell(21)));
        candidate.setMarks8(getSafeCellValue(row.getCell(22)));
        candidate.setMarks9(getSafeCellValue(row.getCell(23)));
        candidate.setMarks10(getSafeCellValue(row.getCell(24)));
        candidate.setBatchId(getSafeCellValue(row.getCell(25)));
        candidate.setState(getSafeCellValue(row.getCell(26)));
        candidate.setDistrict(getSafeCellValue(row.getCell(27)));
        candidate.setPlace(getSafeCellValue(row.getCell(28)));
        candidate.setTemplate(template);
        return candidate;
    }

    private String getSafeCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING: return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) return new SimpleDateFormat("dd-MM-yyyy").format(cell.getDateCellValue());
                    double val = cell.getNumericCellValue();
                    if (val == Math.floor(val)) return String.valueOf((long) val);
                    return String.valueOf(val);
                case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cv = evaluator.evaluate(cell);
                        switch (cv.getCellType()) {
                            case STRING: return cv.getStringValue().trim();
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) return new SimpleDateFormat("dd-MM-yyyy").format(cell.getDateCellValue());
                                double fv = cv.getNumberValue(); if (fv == Math.floor(fv)) return String.valueOf((long) fv); return String.valueOf(fv);
                            case BOOLEAN: return String.valueOf(cv.getBooleanValue());
                            default: return cell.getCellFormula();
                        }
                    } catch (Exception e) { return cell.getCellFormula(); }
                default: return "";
            }
        } catch (Exception e) { return ""; }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        int first = row.getFirstCellNum(); int last = row.getLastCellNum();
        if (first < 0 || last <= 0) return true;
        for (int i = first; i < last; i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK) {
                String v = getSafeCellValue(c);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private boolean isValidCandidate(CandidateDTO candidate) {
        return candidate != null && candidate.getSid() != null && !candidate.getSid().trim().isEmpty() && candidate.getCandidateName() != null && !candidate.getCandidateName().trim().isEmpty();
    }
}
