package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/templates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class TemplateController {

    private final TemplateService templateService;

    // Base folder path where templates are stored
    private final String baseTemplatePath = "C:/certificate_storage/templates/";

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    // Upload a new template with JRXML and optional images
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

    // Fetch all templates
    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates() {
        List<Template> list = templateService.getAllTemplates();
        if (list.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(list);
    }

    // Fetch template by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        try {
            Template template = templateService.getTemplateById(id);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Fetch all images for a template dynamically from folder
    @GetMapping("/{id}/images")
    public ResponseEntity<List<String>> getTemplateImages(@PathVariable Long id) {
        try {
            Template template = templateService.getTemplateById(id);

            // Folder path: base + templateName + /images/
            String folderPath = baseTemplatePath + template.getTemplateName() + "/images/";
            File folder = new File(folderPath);

            if (!folder.exists() || !folder.isDirectory()) return ResponseEntity.ok(List.of());

            // Filter image files
            String[] files = folder.list((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".png") || lower.endsWith(".jpg") ||
                        lower.endsWith(".jpeg") || lower.endsWith(".gif");
            });

            // Prepare URLs for frontend
            List<String> urls = Arrays.stream(files != null ? files : new String[0])
                    .map(f -> "http://localhost:8086/templates/" + template.getTemplateName() + "/images/" + f)
                    .toList();

            return ResponseEntity.ok(urls);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }
}
