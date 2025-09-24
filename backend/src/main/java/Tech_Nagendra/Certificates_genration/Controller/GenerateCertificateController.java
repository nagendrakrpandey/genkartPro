package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.Candidate;
import Tech_Nagendra.Certificates_genration.Repository.ReportRepository;
import Tech_Nagendra.Certificates_genration.Service.CandidateList;
import Tech_Nagendra.Certificates_genration.Utility.Utility;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import net.sf.jasperreports.engine.*;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RestController
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class GenerateCertificateController {

    private final ReportRepository reportRepository;

    private File file = null;
    private File zipfile = null;
    private File logoImage = null;
    private File signImage = null;
    private List<Candidate> failedCandidateList = new ArrayList<>();

    private static final AtomicInteger progressPercentage = new AtomicInteger(0);
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String NUMBER_PATTERN = "#,##0.##";

    private final Path baseFilesPath;

    public GenerateCertificateController(ReportRepository reportRepository,
                                         @Value("${app.catalina.base:${java.io.tmpdir}}") String catalinaBase) {
        this.reportRepository = reportRepository;
        this.baseFilesPath = Paths.get(catalinaBase);
    }

    // ------------------ MAIN GENERATE & DOWNLOAD ------------------
    @PostMapping("/generateCertificate/{templateId}/{userId}/download")
    public ResponseEntity<byte[]> handleGenerateAndDownload(
            @RequestParam(value = "file", required = false) MultipartFile mulfile,
            @RequestParam(value = "zipfile", required = false) MultipartFile mulzipfile,
            @RequestParam(value = "logoImage", required = false) MultipartFile logoImageFile,
            @RequestParam(value = "signImage", required = false) MultipartFile signImageFile,
            HttpServletRequest request,
            @PathVariable String templateId,
            @PathVariable Integer userId) throws IOException {

        try {
            failedCandidateList = new ArrayList<>();
            progressPercentage.set(10);

            // ---------------- TEMP STORE FILES ----------------
            if (mulfile != null && !mulfile.isEmpty()) {
                file = new File(System.getProperty("java.io.tmpdir"), mulfile.getOriginalFilename());
                FileUtils.writeByteArrayToFile(file, mulfile.getBytes());
            } else return ResponseEntity.badRequest().body(null);

            if (mulzipfile != null && !mulzipfile.isEmpty()) {
                zipfile = new File(baseFilesPath.resolve("files/output").toFile(), mulzipfile.getOriginalFilename());
                FileUtils.writeByteArrayToFile(zipfile, mulzipfile.getBytes());
                unpackZip(zipfile, baseFilesPath.resolve("files/output").toFile());
            }

            if (logoImageFile != null && !logoImageFile.isEmpty()) {
                logoImage = new File(baseFilesPath.resolve("files/output/logoSignImages").toFile(), logoImageFile.getOriginalFilename());
                logoImage.getParentFile().mkdirs();
                FileUtils.writeByteArrayToFile(logoImage, logoImageFile.getBytes());
            }

            if (signImageFile != null && !signImageFile.isEmpty()) {
                signImage = new File(baseFilesPath.resolve("files/output/logoSignImages").toFile(), signImageFile.getOriginalFilename());
                signImage.getParentFile().mkdirs();
                FileUtils.writeByteArrayToFile(signImage, signImageFile.getBytes());
            }

            // ---------------- PROCESS FILE ----------------
            String zipPath = processFile(templateId, userId);

            // ---------------- SEND ZIP AS RESPONSE ----------------
            byte[] data = Files.readAllBytes(Paths.get(zipPath));

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Certificates.zip");
            headers.set(HttpHeaders.CONTENT_TYPE, "application/zip");
            headers.setContentLength(data.length);

            return new ResponseEntity<>(data, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ------------------ PROCESS FILE ------------------
    private String processFile(String certificateType, int loggedInUserId) throws IOException {
        List<Candidate> candidates = Utility.getCandidatesFromFile(file);
        if (candidates.isEmpty()) return "Candidate Data not found, Please check Sheet name.";

        File folder = baseFilesPath.resolve("Candidate Image Certificates").toFile();
        folder.mkdirs();

        for (Candidate candidate : candidates) {
            List<Candidate> uni = new ArrayList<>();
            uni.add(candidate);
            CandidateList cl = new CandidateList();
            cl.setCandidates(uni);
            String xml = Utility.convertObjectToXML(cl, CandidateList.class);
            int certType = Integer.parseInt(certificateType);

            generateImageEnabledCertificates(xml, certType, candidate);
            saveImagePDFs(null, candidate, certType, loggedInUserId);
        }

        // pack folder into zip
        File zipOut = baseFilesPath.resolve("Certificates.zip").toFile();
        packZip(folder, zipOut);

        // cleanup temp folder
        FileUtils.deleteDirectory(folder);

        return zipOut.getAbsolutePath();
    }

    // ------------------ PDF GENERATION PLACEHOLDER ------------------
    protected String generateImageEnabledCertificates(String xml, int templateId, Candidate candidate) throws IOException {
        // Tumhara existing JasperReports PDF generation logic yaha implement hoga
        return "";
    }

    protected String saveImagePDFs(String pdfPath, Candidate candidate, Integer certType, int loggedInUserId) {
        // Tumhara existing report save logic
        return "";
    }

    // ------------------ ZIP UTILITIES ------------------
    private void unpackZip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) outFile.mkdirs();
                else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    private void packZip(File sourceDir, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFolder(sourceDir, sourceDir, zos);
        }
    }

    private void zipFolder(File rootDir, File srcDir, ZipOutputStream zos) throws IOException {
        for (File file : Objects.requireNonNull(srcDir.listFiles())) {
            if (file.isDirectory()) zipFolder(rootDir, file, zos);
            else {
                String name = rootDir.toPath().relativize(file.toPath()).toString();
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(name));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                    zos.closeEntry();
                }
            }
        }
    }
}
