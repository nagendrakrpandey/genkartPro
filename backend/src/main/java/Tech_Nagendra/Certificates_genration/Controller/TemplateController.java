package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/templates")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Upload a new template with JRXML file and optional images
     */
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

    /**
     * Fetch all templates
     */
    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates() {
        List<Template> list = templateService.getAllTemplates();
        if (list.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(list);
    }

    /**
     * Fetch template by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        try {
            Template template = templateService.getTemplateById(id);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
