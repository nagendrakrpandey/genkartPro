package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Service.CertificateService;
import Tech_Nagendra.Certificates_genration.Repository.ReportRepository;
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
    private final ReportRepository reportRepository;

    @Value("${certificate.temp.path:C:/certificate_storage/certificates}")
    private String tempPath;

    public CertificateController(CertificateService certificateService,
                                 ReportRepository reportRepository) {
        this.certificateService = certificateService;
        this.reportRepository = reportRepository;
    }

    @PostMapping(value = "/generate-zip/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateCertificatesZip(
            @PathVariable Long templateId,
            @RequestPart("excel") MultipartFile excelFile,
            @RequestPart(value = "zipImage", required = false) MultipartFile zipImage,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "sign", required = false) MultipartFile sign,
            @RequestParam("userId") Long userId) throws Exception {

        // Temporary directory create
        File dir = new File(tempPath);
        if (!dir.exists()) dir.mkdirs();

        // Save Excel file temporarily
        File tempExcel = new File(dir, excelFile.getOriginalFilename());
        try (InputStream in = excelFile.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempExcel)) {
            in.transferTo(fos);
        }

        // Save uploaded files temporarily
        Map<String, File> uploadedFiles = new HashMap<>();
        if (zipImage != null && !zipImage.isEmpty()) {
            File tempZip = new File(dir, zipImage.getOriginalFilename());
            try (InputStream in = zipImage.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempZip)) {
                in.transferTo(fos);
            }
            uploadedFiles.put("zipImage", tempZip);
        }
        if (logo != null && !logo.isEmpty()) {
            File tempLogo = new File(dir, logo.getOriginalFilename());
            try (InputStream in = logo.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempLogo)) {
                in.transferTo(fos);
            }
            uploadedFiles.put("logo", tempLogo);
        }
        if (sign != null && !sign.isEmpty()) {
            File tempSign = new File(dir, sign.getOriginalFilename());
            try (InputStream in = sign.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempSign)) {
                in.transferTo(fos);
            }
            uploadedFiles.put("sign", tempSign);
        }

        // Call service to generate certificates
        Map<String, Object> result = certificateService.generateCertificatesAndReports(
                templateId,
                tempExcel,
                uploadedFiles.isEmpty() ? null : uploadedFiles,
                tempPath,
                userId
        );

        // Extract PDFs and candidates
        @SuppressWarnings("unchecked")
        List<File> pdfFiles = (List<File>) result.get("pdfFiles");
        @SuppressWarnings("unchecked")
        List<CandidateDTO> candidates = (List<CandidateDTO>) result.get("candidates");

        // Save report info in DB
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
            reportRepository.save(report);
        }

        // Create ZIP of PDFs
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

        // Prepare response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("certificates.zip").build());

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }
}
