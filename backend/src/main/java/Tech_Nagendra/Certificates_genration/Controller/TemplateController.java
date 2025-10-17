package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Dto.TemplateDto;
import Tech_Nagendra.Certificates_genration.Service.ProfileService;
import Tech_Nagendra.Certificates_genration.Service.TemplateService;
import Tech_Nagendra.Certificates_genration.Utility.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;
    private final ProfileService profileService;

    @Value("${certificate.template.path:C:/certificate_storage/templates/}")
    private String templateBasePath;

    public TemplateController(TemplateService templateService, JwtUtil jwtUtil, ProfileService profileService) {
        this.templateService = templateService;
        this.jwtUtil = jwtUtil;
        this.profileService = profileService;
    }

    // Safe token extraction from header
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7).trim();
    }

    // Upload template
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadTemplate(
            @RequestParam("templateName") String templateName,
            @RequestParam("imageType") Integer imageType,
            @RequestPart("jrxml") MultipartFile jrxml,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            HttpServletRequest request
    ) {
        try {
            String token = extractToken(request);
            Long userId = jwtUtil.extractUserId(token);
            if (userId == null) return ResponseEntity.status(401).body("Invalid token: user not found");

            TemplateDto saved = templateService.saveTemplate(userId, templateName, imageType, jrxml, images);
            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to upload template: " + e.getMessage());
        }
    }

    // Get all templates
    @GetMapping
    public ResponseEntity<List<TemplateDto>> getAllTemplates(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            if (userId == null) return ResponseEntity.status(401).body(Collections.emptyList());

            List<TemplateDto> templates = templateService.getAllTemplates(userId, role);
            if (templates.isEmpty()) return ResponseEntity.noContent().build();

            return ResponseEntity.ok(templates);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // Get template by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            if (userId == null) return ResponseEntity.status(401).body("Invalid token: user not found");

            TemplateDto template = templateService.getTemplateByIdForUser(id, userId, role);
            if (template == null) return ResponseEntity.status(404).body("Template not found");
            return ResponseEntity.ok(template);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching template: " + e.getMessage());
        }
    }

    // Get template images
    @GetMapping("/{templateId}/images")
    public ResponseEntity<List<String>> getTemplateImages(@PathVariable Long templateId, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            if (userId == null) return ResponseEntity.status(401).body(Collections.emptyList());

            TemplateDto template = templateService.getTemplateByIdForUser(templateId, userId, role);
            if (template == null) return ResponseEntity.status(404).body(Collections.emptyList());

            File folder = new File(templateBasePath + template.getTemplateName() + "/");
            if (!folder.exists() || !folder.isDirectory()) return ResponseEntity.ok(Collections.emptyList());

            List<String> imageUrls = new ArrayList<>();
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                        String url = "http://localhost:8086/templates/images/" + template.getTemplateName() + "/" + file.getName();
                        imageUrls.add(url);
                    }
                }
            }
            return ResponseEntity.ok(imageUrls);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // Get total templates count
    @GetMapping("/count")
    public ResponseEntity<?> getTotalTemplates(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            if (userId == null) return ResponseEntity.status(401).body("Invalid token: user not found");

            Long count = templateService.getTotalTemplates(userId, role);
            return ResponseEntity.ok(count);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch template count: " + e.getMessage());
        }
    }
}
