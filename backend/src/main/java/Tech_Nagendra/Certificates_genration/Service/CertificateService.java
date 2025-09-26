package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.CandidateDTO;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Utility.Utility;
import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CertificateService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static File file = null;
    private static File zipfile = null;
    private static File logoImage = null;
    private static File signImage = null;
    private static List<CandidateDTO> failedCandidateList = new ArrayList<>();
    private static int progressPercentage = 0;

    // Persistent folder for generated certificates
    private static final String CERTIFICATE_BASE_DIR = "C:/certificate_storage/Candidate_Certificates/";

    // ---------------- Generate Certificates ----------------
    public String generateCertificates(MultipartFile excelFile, MultipartFile zip,
                                       MultipartFile logo, MultipartFile sign,
                                       String templateId, Integer userId) throws Exception {

        failedCandidateList = new ArrayList<>();
        progressPercentage = 10;

        // Create base folder if it does not exist
        File baseFolder = new File(CERTIFICATE_BASE_DIR);
        if (!baseFolder.exists()) baseFolder.mkdirs();

        // Save Excel file
        file = new File(CERTIFICATE_BASE_DIR + excelFile.getOriginalFilename());
        FileUtils.writeByteArrayToFile(file, excelFile.getBytes());

        // Save ZIP file if provided
        if (zip != null && !"".equals(zip.getOriginalFilename())) {
            zipfile = new File(CERTIFICATE_BASE_DIR + zip.getOriginalFilename());
            FileUtils.writeByteArrayToFile(zipfile, zip.getBytes());
            ZipUtil.unpack(zipfile, baseFolder);
        }

        // Save Logo
        if (logo != null && !"".equals(logo.getOriginalFilename())) {
            logoImage = new File(CERTIFICATE_BASE_DIR + "logoSignImages/" + logo.getOriginalFilename());
            logoImage.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(logoImage, logo.getBytes());
        }

        // Save Sign
        if (sign != null && !"".equals(sign.getOriginalFilename())) {
            signImage = new File(CERTIFICATE_BASE_DIR + "logoSignImages/" + sign.getOriginalFilename());
            signImage.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(signImage, sign.getBytes());
        }

        // Process and generate certificates
        return processFile(templateId, userId);
    }

    // ---------------- Process Certificates ----------------
    private String processFile(String templateId, int userId) throws Exception {
        List<CandidateDTO> candidates = Utility.getCandidatesFromFile(file);
        if (candidates.isEmpty()) return "Candidate Data not found.";

        File outputFolder = new File(CERTIFICATE_BASE_DIR + "Output/");
        if (!outputFolder.exists()) outputFolder.mkdirs();

        if (zipfile != null || logoImage != null || signImage != null) { // Image-enabled
            for (CandidateDTO candidate : candidates) {
                List<CandidateDTO> uniCandidateList = Collections.singletonList(candidate);
                String xml = Utility.convertObjectToXML(uniCandidateList, Object.class);
                try {
                    String pdfPath = Utility.generateImageEnabledCertificates(xml, Integer.parseInt(templateId), candidate, logoImage, signImage);

                    // Save each PDF to persistent folder
                    File destPdf = new File(outputFolder, candidate.getCandidateName() + ".pdf");
                    FileUtils.copyFile(new File(pdfPath), destPdf);

                } catch (Exception e) {
                    failedCandidateList.add(candidate);
                }
                if (progressPercentage <= 90) progressPercentage += 5;
            }
            progressPercentage = 90;

            writeFailedCandidateList(outputFolder.getAbsolutePath());

            // Zip all generated PDFs
            File zipFile = new File(CERTIFICATE_BASE_DIR + "Certificates.zip");
            ZipUtil.pack(outputFolder, zipFile);
            progressPercentage = 100;

            return CERTIFICATE_BASE_DIR + "Certificates.zip";
        } else { // Normal certificates (Excel only)
            String xml = Utility.convertObjectToXML(candidates, Object.class);
            String pdfPath = Utility.generateCertificateXML(xml, Integer.parseInt(templateId));

            File destPdf = new File(outputFolder, "Certificates.pdf");
            FileUtils.copyFile(new File(pdfPath), destPdf);

            return destPdf.getAbsolutePath();
        }
    }

    // ---------------- Write Failed Candidates ----------------
    private void writeFailedCandidateList(String folderPath) throws IOException {
        progressPercentage = 95;
        String filepath = folderPath + "/Failed Candidate List.txt";
        FileWriter writer = new FileWriter(filepath);
        for (CandidateDTO candidate : failedCandidateList) {
            writer.write(candidate.getSid() + " - " + candidate.getCandidateName());
            writer.write(System.lineSeparator());
        }
        writer.close();
    }

    // ---------------- Read File Bytes ----------------
    public byte[] readFileBytes(File file) throws IOException {
        return FileUtils.readFileToByteArray(file);
    }

    // ---------------- Progress ----------------
    public int getProgress() {
        return progressPercentage;
    }

    public void setProgress(int value) {
        progressPercentage = value;
    }

    // ---------------- Fetch Templates by User ----------------
    public Template[] getTemplatesByUser(Integer userId) throws Exception {
        String url = "http://localhost:8086/templates?userId=" + userId;
        ResponseEntity<Template[]> response = restTemplate.getForEntity(url, Template[].class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new Exception("Failed to fetch templates for user " + userId);
        }
    }
}
