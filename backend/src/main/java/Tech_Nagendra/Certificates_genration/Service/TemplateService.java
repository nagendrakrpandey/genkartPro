package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    // Save new template with automatic common images
    public Template saveTemplate(String templateName, Integer imageType, Long userId,
                                 MultipartFile jrxml, MultipartFile[] images) throws IOException {
        // 1️⃣ Create template folder
        File folder = new File("templates/" + templateName);
        if (!folder.exists()) folder.mkdirs();

        // 2️⃣ Save JRXML
        File jrxmlFile = new File(folder, jrxml.getOriginalFilename());
        jrxml.transferTo(jrxmlFile);

        // 3️⃣ Save template-specific images
        List<String> allImagePaths = new ArrayList<>();
        for (MultipartFile img : images) {
            File imgFile = new File(folder, img.getOriginalFilename());
            img.transferTo(imgFile);
            allImagePaths.add(imgFile.getAbsolutePath());
        }

        // 4️⃣ Add common images automatically
        File commonDir = new File("C:/certificate_storage/common_images/");
        if (commonDir.exists() && commonDir.isDirectory()) {
            for (File commonImg : Objects.requireNonNull(commonDir.listFiles())) {
                allImagePaths.add(commonImg.getAbsolutePath());
            }
        }

        // 5️⃣ Save template entity
        Template template = new Template();
        template.setTemplateName(templateName);
        template.setImageType(imageType);
        template.setUser_id(userId);
        template.setTemplateFolder(folder.getAbsolutePath());
        template.setCommon_images(String.join(",", allImagePaths));
        template.setJrxmlPath(jrxmlFile.getAbsolutePath());

        return templateRepository.save(template);
    }

    // Fetch all templates
    public List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }

    // Fetch template by ID
    public Template getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    // Fetch template by name
    public Template getTemplateByName(String templateName) {
        return templateRepository.findByTemplateName(templateName)
                .orElse(null);
    }

    // Fetch images for a template with fallback to common images
    public List<File> getTemplateImagesWithFallback(Long templateId) throws IOException {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        List<File> imagesToUse = new ArrayList<>();
        String[] requiredImages = {"img1.png", "img2.png", "img3.png"};

        File templateFolder = new File("templates/" + template.getTemplateName());

        // Fetch from template folder first
        for (String imgName : requiredImages) {
            File imgFile = new File(templateFolder, imgName);
            if (imgFile.exists()) {
                imagesToUse.add(imgFile);
            } else {
                // fallback: common_images column
                if (template.getCommon_images() != null) {
                    String[] commonPaths = template.getCommon_images().split(",");
                    boolean found = false;
                    for (String path : commonPaths) {
                        File f = new File(path.trim());
                        if (f.exists() && f.getName().equals(imgName)) {
                            imagesToUse.add(f);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new RuntimeException("Missing image: " + imgName);
                    }
                } else {
                    throw new RuntimeException("Missing image: " + imgName);
                }
            }
        }

        // Add remaining images from template folder if any
        if (templateFolder.exists()) {
            File[] remaining = templateFolder.listFiles((dir, name) ->
                    imagesToUse.stream().noneMatch(f -> f.getName().equals(name))
            );
            if (remaining != null) imagesToUse.addAll(Arrays.asList(remaining));
        }

        return imagesToUse;
    }
}
