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

    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

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

    @GetMapping
    public ResponseEntity<List<Template>> getTemplates() {
        List<Template> templates = service.getAllTemplates();
        if (templates.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        try {
            Template template = service.getTemplateById(id);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}


