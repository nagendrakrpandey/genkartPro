package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Service.CertificateService;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class BulkCertificateController {

    @Autowired
    private CertificateService certificateService;

    @PostMapping("/generateCertificate/{templateId}/{userId}/{imageType}")
    public ResponseEntity<String> handleGenerateCertificate(
            @RequestParam("file") MultipartFile excelFile,
            @RequestParam(value = "zipfile", required = false) MultipartFile zipFile,
            @RequestParam(value = "logoImage", required = false) MultipartFile logoImageFile,
            @RequestParam(value = "signImage", required = false) MultipartFile signImageFile,
            @PathVariable String templateId,
            @PathVariable Integer userId,
            @PathVariable Integer imageType
    ) {
        try {
            if (excelFile == null || excelFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Excel file is required for all image types.");
            }
            if (imageType >= 1 && (zipFile == null || zipFile.isEmpty())) {
                return ResponseEntity.badRequest().body("ZIP file is required for imageType 1, 2, 3.");
            }
            if (imageType >= 2 && (logoImageFile == null || logoImageFile.isEmpty())) {
                return ResponseEntity.badRequest().body("Logo image is required for imageType 2 and 3.");
            }
            if (imageType == 3 && (signImageFile == null || signImageFile.isEmpty())) {
                return ResponseEntity.badRequest().body("Signature image is required for imageType 3.");
            }

            certificateService.setProgress(10);
            String generatedPath = certificateService.generateCertificates(
                    excelFile, zipFile, logoImageFile, signImageFile, templateId, userId
            );

            return ResponseEntity.ok("Certificate generated successfully. Path: " + generatedPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Certificate Generation Error: " + e.getMessage());
        }
    }

    // ---------------- Download ZIP ----------------
    @GetMapping("/downloadCertificateZip")
    public ResponseEntity<byte[]> downloadCertificateZip() {
        try {
            File zip = new File("C:/certificate_storage/Candidate_Certificates/Certificates.zip");
            byte[] data = certificateService.readFileBytes(zip);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(data.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Certificates.zip");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/loader/getProgressBarPercentage")
    public Integer getProgressBarPercentage() {
        return certificateService.getProgress();
    }

    @GetMapping("/templates/{userId}")
    public ResponseEntity<Template[]> getAssignedTemplates(@PathVariable Integer userId) {
        try {
            Template[] templates = certificateService.getTemplatesByUser(userId);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
