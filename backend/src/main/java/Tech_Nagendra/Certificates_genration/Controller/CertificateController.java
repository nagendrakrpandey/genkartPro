package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Service.CertificateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class CertificateController {

    private final CertificateService certificateService;

    @Value("${certificate.temp.path:C:/certificate_storage/certificates}")
    private String tempPath;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Generate certificates as individual PDFs and return as a ZIP
     */
    @PostMapping(value = "/generate-zip/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateCertificatesZip(
            @PathVariable Long templateId,
            @RequestPart("excel") MultipartFile excelFile) throws Exception {

        // Ensure temp directory exists
        File dir = new File(tempPath);
        if (!dir.exists()) dir.mkdirs();

        // Save uploaded Excel temporarily
        File tempExcel = new File(dir, System.currentTimeMillis() + "_" + excelFile.getOriginalFilename());
        try (InputStream in = excelFile.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempExcel)) {
            in.transferTo(fos);
        }

        // Generate PDFs
        List<File> pdfFiles = certificateService.generateCertificatesToLocal(templateId, tempExcel, tempPath);

        // Create ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (File pdf : pdfFiles) {
                zos.putNextEntry(new ZipEntry(pdf.getName()));
                Files.copy(pdf.toPath(), zos);
                zos.closeEntry();
            }
        }

        // Cleanup: delete temporary Excel & PDFs
        tempExcel.delete();
        for (File pdf : pdfFiles) pdf.delete();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("certificates.zip").build());

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }
}
