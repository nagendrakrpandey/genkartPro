package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Service.TemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/templates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class TemplateController {

    private final TemplateService templateService;

    @Value("${certificate.template.path:C:/certificate_storage/templates/}")
    private String templateBasePath;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadTemplate(
            @RequestParam("templateName") String templateName,
            @RequestParam("imageType") Integer imageType,
            @RequestParam("userId") Long userId,
            @RequestPart("jrxml") MultipartFile jrxml,
            @RequestPart(value = "images", required = false) MultipartFile[] images
    ) {
        try {
            Template saved = templateService.saveTemplate(templateName, imageType, userId, jrxml, images);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to upload template: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates() {
        List<Template> list = templateService.getAllTemplates();
        if (list.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        try {
            Template template = templateService.getTemplateById(id);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ✅ NEW: Endpoint to fetch template images dynamically
    @GetMapping("/{templateId}/images")
    public ResponseEntity<List<String>> getTemplateImages(@PathVariable Long templateId) {
        try {
            Template template = templateService.getTemplateById(templateId);
            if (template == null) {
                return ResponseEntity.status(404).body(Collections.emptyList());
            }

            // Build folder path (where template images are stored)
            File folder = new File(templateBasePath + templateId + "/");
            if (!folder.exists() || !folder.isDirectory()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Collect all image URLs
            List<String> imageUrls = new ArrayList<>();
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                        String url = "http://localhost:8086/templates/images/" + templateId + "/" + file.getName();
                        imageUrls.add(url);
                    }
                }
            }

            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
}
