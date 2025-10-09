package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Service.CertificateService;
import Tech_Nagendra.Certificates_genration.Service.ReportService;
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

    @Value("${certificate.temp.path:C:/certificate_storage/certificates}")
    private String tempPath;

    public CertificateController(CertificateService certificateService,
                                 ReportService reportService) {
        this.certificateService = certificateService;
        this.reportService = reportService;
    }

    @PostMapping(value = "/generate-zip/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateCertificatesZip(
            @PathVariable Long templateId,
            @RequestPart("excel") MultipartFile excelFile,
            @RequestPart(value = "zipImage", required = false) MultipartFile zipImage,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "sign", required = false) MultipartFile sign,
            @RequestParam("userId") Long userId) throws Exception {

        File dir = new File(tempPath);
        if (!dir.exists()) dir.mkdirs();

        // Save uploaded Excel temporarily
        File tempExcel = new File(dir, excelFile.getOriginalFilename());
        try (InputStream in = excelFile.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempExcel)) {
            in.transferTo(fos);
        }

        Map<String, File> uploadedFiles = new HashMap<>();
        if (zipImage != null && !zipImage.isEmpty()) saveTempFile(uploadedFiles, zipImage, dir, "zipImage");
        if (logo != null && !logo.isEmpty()) saveTempFile(uploadedFiles, logo, dir, "logo");
        if (sign != null && !sign.isEmpty()) saveTempFile(uploadedFiles, sign, dir, "sign");

        // Generate certificates and get PDF files and candidate data
        Map<String, Object> result = certificateService.generateCertificatesAndReports(
                templateId,
                tempExcel,
                uploadedFiles.isEmpty() ? null : uploadedFiles,
                tempPath,
                userId
        );

        List<File> pdfFiles = (List<File>) result.get("pdfFiles");
        List<CandidateDTO> candidates = (List<CandidateDTO>) result.get("candidates");

        Date now = new Date();
        for (CandidateDTO candidate : candidates) {
            Report report = new Report();
            report.setSid(candidate.getSid());
            report.setGeneratedBy(userId);
            report.setGeneratedOn(now);
            report.setJobrole(candidate.getJobRole());
            report.setCourseName(candidate.getSector());
            report.setLevel(candidate.getLevel());
            report.setTemplateID(templateId);
            report.setTrainingPartner(candidate.getSectorSkillCouncil());
            report.setBatchId(candidate.getBatchId());
            report.setGrade(candidate.getGrade());
            report.setTemplateName(candidate.getTemplate() != null ? candidate.getTemplate().getTemplateName() : null);


            reportService.saveOrUpdateBySid(report);
        }

        // Create ZIP from PDF files
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (File pdf : pdfFiles) {
                if (pdf.exists()) {
                    zos.putNextEntry(new ZipEntry(pdf.getName()));
                    Files.copy(pdf.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }

        // Cleanup temporary files
        tempExcel.delete();
        uploadedFiles.values().forEach(File::delete);
        pdfFiles.forEach(File::delete);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("certificates.zip").build());

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    private void saveTempFile(Map<String, File> uploadedFiles, MultipartFile file, File dir, String key) throws IOException {
        File temp = new File(dir, file.getOriginalFilename());
        try (InputStream in = file.getInputStream(); FileOutputStream fos = new FileOutputStream(temp)) {
            in.transferTo(fos);
        }
        uploadedFiles.put(key, temp);
    }

    @GetMapping("/reports/all")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/reports/user/{userId}")
    public ResponseEntity<List<Report>> getReportsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(reportService.getReportsByUser(userId));
    }

    @GetMapping("/reports/count/user/{userId}")
    public ResponseEntity<Long> countReportsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(reportService.countCertificatesByUser(userId));
    }

    @GetMapping("/reports/count/month")
    public ResponseEntity<Long> countReportsThisMonth() {
        return ResponseEntity.ok(reportService.countCertificatesThisMonth());
    }
}
