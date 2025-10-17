package Tech_Nagendra.Certificates_genration.Controller;
import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Entity.UserProfile;
import Tech_Nagendra.Certificates_genration.Security.UserPrincipal;
import Tech_Nagendra.Certificates_genration.Service.CertificateService;
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

    @Value("${certificate.temp.path:C:/certificate_storage/certificates}")
    private String tempPath;

    public CertificateController(CertificateService certificateService,
                                 ReportService reportService,
                                 ProfileRepository profileRepository,
                                 JwtUtil jwtUtil) {
        this.certificateService = certificateService;
        this.reportService = reportService;
        this.profileRepository = profileRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value = "/generate-zip/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateCertificatesZip(
            @PathVariable Long templateId,
            @RequestPart("excel") MultipartFile excelFile,
            @RequestPart(value = "zipImage", required = false) MultipartFile zipImage,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "sign", required = false) MultipartFile sign,
            @RequestHeader("Authorization") String tokenHeader) throws Exception {

        // Extract userId from token
        String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
        Long userId = jwtUtil.extractUserId(token);

        UserProfile userProfile = profileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Convert UserProfile to UserPrincipal
        UserPrincipal currentUser = new UserPrincipal(userProfile);

        // Create temp folder
        File dir = new File(tempPath);
        if (!dir.exists()) dir.mkdirs();

        // Save Excel to temp
        File tempExcel = new File(dir, System.currentTimeMillis() + "_" + excelFile.getOriginalFilename());
        try (InputStream in = excelFile.getInputStream(); FileOutputStream fos = new FileOutputStream(tempExcel)) {
            in.transferTo(fos);
        }

        // Save uploaded files
        Map<String, File> uploadedFiles = new HashMap<>();
        if (zipImage != null && !zipImage.isEmpty()) saveTempFile(uploadedFiles, zipImage, dir, "zipImage");
        if (logo != null && !logo.isEmpty()) saveTempFile(uploadedFiles, logo, dir, "logo");
        if (sign != null && !sign.isEmpty()) saveTempFile(uploadedFiles, sign, dir, "sign");

        // Generate certificates and reports
        Map<String, Object> result = certificateService.generateCertificatesAndReports(
                templateId,
                tempExcel,
                uploadedFiles.isEmpty() ? null : uploadedFiles,
                tempPath,
                currentUser
        );

        List<File> pdfFiles = (List<File>) result.get("pdfFiles");
        List<CandidateDTO> candidates = (List<CandidateDTO>) result.get("candidates");

        for (CandidateDTO candidate : candidates) {
            reportService.saveCandidateReport(candidate, templateId, userId, currentUser);
        }

        // Create ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int size = Math.min(pdfFiles.size(), candidates.size());
            for (int i = 0; i < size; i++) {
                File pdf = pdfFiles.get(i);
                CandidateDTO candidate = candidates.get(i);
                if (pdf.exists()) {
                    String safeName = candidate.getCandidateName() != null
                            ? candidate.getCandidateName().replaceAll("[^a-zA-Z0-9]", "_")
                            : "Candidate";
                    String zipEntryName = safeName + "_" + candidate.getSid() + ".pdf";
                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(pdf.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }

        // Cleanup temp files
        tempExcel.delete();
        uploadedFiles.values().forEach(File::delete);
        pdfFiles.forEach(File::delete);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("certificates.zip").build());

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    private void saveTempFile(Map<String, File> uploadedFiles, MultipartFile file, File dir, String key) throws IOException {
        File temp = new File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
        try (InputStream in = file.getInputStream(); FileOutputStream fos = new FileOutputStream(temp)) {
            in.transferTo(fos);
        }
        uploadedFiles.put(key, temp);
    }

    @GetMapping("/reports/all")
    public ResponseEntity<List<Report>> getAllReports(@RequestHeader("Authorization") String tokenHeader) {
        String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
        Long userId = jwtUtil.extractUserId(token);

        UserProfile userProfile = profileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPrincipal currentUser = new UserPrincipal(userProfile);

        return ResponseEntity.ok(reportService.getAllReports(currentUser));
    }

    @GetMapping("/reports/count/month")
    public ResponseEntity<Long> countReportsThisMonth() {
        return ResponseEntity.ok(reportService.countCertificatesThisMonth());
    }
}
