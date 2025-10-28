package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Entity.UserProfile;
import Tech_Nagendra.Certificates_genration.Security.UserPrincipal;
import Tech_Nagendra.Certificates_genration.Service.CertificateService;
import Tech_Nagendra.Certificates_genration.Service.DynamicFontService;
import Tech_Nagendra.Certificates_genration.Service.ReportService;
import Tech_Nagendra.Certificates_genration.Repository.ProfileRepository;
import Tech_Nagendra.Certificates_genration.Utility.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class CertificateController {

    private final CertificateService certificateService;
    private final ReportService reportService;
    private final ProfileRepository profileRepository;
    private final JwtUtil jwtUtil;
    private final DynamicFontService dynamicFontService;

    @Value("${certificate.temp.path:C:/certificate_storage/certificates}")
    private String tempPath;

    public CertificateController(CertificateService certificateService,
                                 ReportService reportService,
                                 ProfileRepository profileRepository,
                                 JwtUtil jwtUtil,
                                 DynamicFontService dynamicFontService) {
        this.certificateService = certificateService;
        this.reportService = reportService;
        this.profileRepository = profileRepository;
        this.jwtUtil = jwtUtil;
        this.dynamicFontService = dynamicFontService;
    }

    @PostMapping(value = "/generate-zip/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateCertificatesZip(
            @PathVariable Long templateId,
            @RequestPart("excel") MultipartFile excelFile,
            @RequestPart(value = "zipImage", required = false) MultipartFile zipImage,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "sign", required = false) MultipartFile sign,
            @RequestHeader("Authorization") String tokenHeader) {

        File tempExcel = null;
        Map<String, File> uploadedFiles = new HashMap<>();
        File dir = null;

        try {
            if (excelFile == null || excelFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Excel file is required"));
            }

            String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
            Long userId = jwtUtil.extractUserId(token);

            UserProfile userProfile = profileRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            UserPrincipal currentUser = new UserPrincipal(userProfile);

            dir = new File(tempPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create temp directory: " + tempPath);
                }
            }

            tempExcel = new File(dir, System.currentTimeMillis() + "_" + excelFile.getOriginalFilename());
            try (InputStream in = excelFile.getInputStream(); FileOutputStream fos = new FileOutputStream(tempExcel)) {
                in.transferTo(fos);
            }

            if (zipImage != null && !zipImage.isEmpty()) saveTempFile(uploadedFiles, zipImage, dir, "zipImage");
            if (logo != null && !logo.isEmpty()) saveTempFile(uploadedFiles, logo, dir, "logo");
            if (sign != null && !sign.isEmpty()) saveTempFile(uploadedFiles, sign, dir, "sign");

            Map<String, Object> result = certificateService.generateCertificatesAndReports(
                    templateId,
                    tempExcel,
                    uploadedFiles.isEmpty() ? null : uploadedFiles,
                    tempPath,
                    currentUser
            );

            if (result.containsKey("error") && (Boolean) result.get("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Certificate generation failed", "message", result.get("message")));
            }

            List<File> pdfFiles = (List<File>) result.get("pdfFiles");
            List<CandidateDTO> candidates = (List<CandidateDTO>) result.get("candidates");

            if (pdfFiles == null) pdfFiles = new ArrayList<>();
            if (candidates == null) candidates = new ArrayList<>();

            if (pdfFiles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "No PDF files generated"));
            }

            for (CandidateDTO candidate : candidates) {
                try {
                    reportService.saveCandidateReport(candidate, templateId, userId, currentUser);
                } catch (Exception ignored) { }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                int successfulEntries = 0;
                int size = Math.min(pdfFiles.size(), candidates.size());

                for (int i = 0; i < size; i++) {
                    File pdf = pdfFiles.get(i);
                    CandidateDTO candidate = candidates.get(i);

                    if (pdf != null && pdf.exists()) {
                        try {
                            String safeName = candidate.getCandidateName() != null
                                    ? candidate.getCandidateName().replaceAll("[^a-zA-Z0-9.-]", "_")
                                    : "Candidate";
                            String zipEntryName = safeName + "_" + (candidate.getSid() != null ? candidate.getSid() : "Unknown") + ".pdf";

                            zos.putNextEntry(new ZipEntry(zipEntryName));
                            Files.copy(pdf.toPath(), zos);
                            zos.closeEntry();
                            successfulEntries++;
                        } catch (Exception e) { }
                    }
                }

                if (pdfFiles.size() > candidates.size()) {
                    for (int i = candidates.size(); i < pdfFiles.size(); i++) {
                        File pdf = pdfFiles.get(i);
                        if (pdf != null && pdf.exists()) {
                            try {
                                String zipEntryName = "Certificate_" + (i + 1) + ".pdf";
                                zos.putNextEntry(new ZipEntry(zipEntryName));
                                Files.copy(pdf.toPath(), zos);
                                zos.closeEntry();
                                successfulEntries++;
                            } catch (Exception e) { }
                        }
                    }
                }

                if (successfulEntries == 0) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "No files could be added to ZIP"));
                }
            }

            byte[] zipBytes = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("certificates_" + System.currentTimeMillis() + ".zip")
                    .build());
            headers.setContentLength(zipBytes.length);

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Certificate generation failed", "message", e.getMessage()));
        } finally {
            cleanupTempFiles(tempExcel, uploadedFiles);
        }
    }

    private void saveTempFile(Map<String, File> uploadedFiles, MultipartFile file, File dir, String key) throws IOException {
        File temp = new File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
        try (InputStream in = file.getInputStream(); FileOutputStream fos = new FileOutputStream(temp)) {
            in.transferTo(fos);
        }
        uploadedFiles.put(key, temp);
    }

    private void cleanupTempFiles(File excelFile, Map<String, File> uploadedFiles) {
        try {
            if (excelFile != null && excelFile.exists()) excelFile.delete();
            for (Map.Entry<String, File> entry : uploadedFiles.entrySet()) {
                File file = entry.getValue();
                if (file != null && file.exists()) file.delete();
            }
        } catch (Exception ignored) { }
    }

    @GetMapping("/reports/all")
    public ResponseEntity<?> getAllReports(@RequestHeader("Authorization") String tokenHeader) {
        try {
            String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
            Long userId = jwtUtil.extractUserId(token);

            UserProfile userProfile = profileRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            UserPrincipal currentUser = new UserPrincipal(userProfile);

            List<Report> reports = reportService.getAllReports(currentUser);
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch reports", "message", e.getMessage()));
        }
    }

    @GetMapping("/reports/count/month")
    public ResponseEntity<?> countReportsThisMonth() {
        try {
            Long count = reportService.countCertificatesThisMonth();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to count reports", "message", e.getMessage()));
        }
    }

    @GetMapping("/fonts/status")
    public ResponseEntity<?> getFontStatus() {
        try {
            Map<String, Object> fontInfo = dynamicFontService.getFontInfo();
            return ResponseEntity.ok(fontInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get font status", "message", e.getMessage()));
        }
    }

    @PostMapping("/fonts/reload")
    public ResponseEntity<?> reloadFonts() {
        try {
            dynamicFontService.reloadFonts();
            return ResponseEntity.ok(Map.of("message", "Fonts reloaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reload fonts", "message", e.getMessage()));
        }
    }

    @GetMapping("/fonts/available")
    public ResponseEntity<?> getAvailableFonts() {
        try {
            Set<String> availableFonts = dynamicFontService.getAvailableFontFamilies();
            return ResponseEntity.ok(Map.of("availableFonts", availableFonts, "totalFonts", availableFonts.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get available fonts", "message", e.getMessage()));
        }
    }

    @GetMapping("/fonts/check/{fontName}")
    public ResponseEntity<?> checkFont(@PathVariable String fontName) {
        try {
            boolean isAvailable = dynamicFontService.isFontFamilyAvailable(fontName);
            return ResponseEntity.ok(Map.of("fontName", fontName, "available", isAvailable));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check font", "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "Certificate Generation");
        healthInfo.put("timestamp", new Date());
        healthInfo.put("tempPath", tempPath);

        File tempDir = new File(tempPath);
        healthInfo.put("tempDirectoryExists", tempDir.exists());
        healthInfo.put("tempDirectoryWritable", tempDir.canWrite());

        return ResponseEntity.ok(healthInfo);
    }

    @GetMapping("/status/{templateId}")
    public ResponseEntity<?> getGenerationStatus(@PathVariable Long templateId) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("templateId", templateId);
            status.put("status", "ready");
            status.put("message", "Certificate generation service is available");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Status check failed", "message", e.getMessage()));
        }
    }
}
