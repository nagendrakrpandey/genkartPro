package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/templates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class TemplateController {

    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    // Upload a new template
    @PostMapping
    public ResponseEntity<?> uploadTemplate(
            @RequestParam("templateName") String templateName,
            @RequestParam("imageType") Integer imageType,
            @RequestParam("userId") Long userId,
            @RequestParam("jrxml") MultipartFile jrxml,
            @RequestParam("images") MultipartFile[] images
    ) {
        try {
            Template saved = service.saveTemplate(templateName, imageType, userId, jrxml, images);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to upload template: " + e.getMessage());
        }
    }

    // Get all templates
    @GetMapping
    public ResponseEntity<List<Template>> getTemplates() {
        List<Template> templates = service.getAllTemplates();
        if (templates.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(templates);
    }

    // Get template by name
    @GetMapping("/name/{templateName}")
    public ResponseEntity<?> getTemplateByName(@PathVariable String templateName) {
        try {
            Template template = service.getTemplateByName(templateName);
            if (template == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Unified getImage method (template folder + common images)
    public File getImage(String templateName, String imageName) {
        // 1. Check template folder
        File templateImage = new File("templates/" + templateName + "/" + imageName);
        if (templateImage.exists()) {
            System.out.println("Using template image: " + templateImage.getPath());
            return templateImage;
        }

        // 2. Check common images from column
        Template template = service.getTemplateByName(templateName);
        if (template != null && template.getCommon_images() != null) {
            String[] commonPaths = template.getCommon_images().split(",");
            for (String path : commonPaths) {
                File commonImage = new File(path.trim());
                if (commonImage.exists() && commonImage.getName().equals(imageName)) {
                    System.out.println("Using common image: " + commonImage.getPath());
                    return commonImage;
                }
            }
        }

        System.out.println("Image not found anywhere: " + imageName);
        return null;
    }

    // Generate certificate (returns which images are used)
    public Map<String, String> generateCertificate(String templateName, String[] requiredImages) throws Exception {
        Template template = service.getTemplateByName(templateName);
        if (template == null) {
            throw new Exception("Template not found: " + templateName);
        }

        Map<String, String> usedImages = new HashMap<>();

        for (String imgName : requiredImages) {
            File imgFile = getImage(template.getTemplateName(), imgName);
            if (imgFile != null) {
                usedImages.put(imgName, imgFile.getPath());
            } else {
                usedImages.put(imgName, "MISSING");
            }
        }

        return usedImages;
    }

    // Test endpoint to check which images are used (template + common)
    @GetMapping("/test/{templateName}")
    public ResponseEntity<?> testTemplateImages(@PathVariable String templateName) {
        try {
            Template template = service.getTemplateByName(templateName);
            if (template == null) {
                return ResponseEntity.status(404).body("Template not found: " + templateName);
            }

            String[] images = {"img1.png", "img2.png", "img3.png"}; // Example images
            Map<String, String> result = generateCertificate(templateName, images);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Example bulk certificate generation endpoint
    @PostMapping("/bulk/{templateName}")
    public ResponseEntity<?> generateBulkCertificates(@PathVariable String templateName) {
        try {
            String[] images = {"img1.png", "img2.png", "img3.png"}; // Example images
            Map<String, String> usedImages = generateCertificate(templateName, images);
            return ResponseEntity.ok(usedImages);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // Upload common images to folder
    @PostMapping("/common/upload")
    public ResponseEntity<?> uploadCommonImages(@RequestParam("images") MultipartFile[] images) {
        String commonDir = "C:/certificate_storage/common_images/"; // your common_images folder
        try {
            for (MultipartFile file : images) {
                File dest = new File(commonDir + file.getOriginalFilename());
                file.transferTo(dest);
                System.out.println("Uploaded: " + dest.getAbsolutePath());
            }
            return ResponseEntity.ok("Common images uploaded successfully ✅");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to upload common images: " + e.getMessage());
        }
    }

}
