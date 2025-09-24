package Tech_Nagendra.Certificates_genration.Service;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Value("${file.upload-dir}")
    private String uploadDir;

    public Template saveTemplate(String templateName, Integer imageType, Long userId, MultipartFile jrxml, MultipartFile[] images) throws IOException {
        String folderName = System.currentTimeMillis() + "_" + templateName;

        Path storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path templateDir = storageRoot.resolve(folderName);

        Files.createDirectories(templateDir);

        if (jrxml == null || jrxml.isEmpty()) {
            throw new RuntimeException("JRXML file is missing or empty.");
        }

        Path jrxmlPath = templateDir.resolve(jrxml.getOriginalFilename());
        jrxml.transferTo(jrxmlPath.toFile());

        if (images != null) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String filename = Optional.ofNullable(image.getOriginalFilename())
                            .filter(f -> !f.trim().isEmpty())
                            .orElse("image_" + System.currentTimeMillis() + ".png");
                    Path imagePath = templateDir.resolve(filename);
                    image.transferTo(imagePath.toFile());
                }
            }
        }

        Template template = new Template();
        template.setTemplateName(templateName);
        template.setImageType(imageType);
        template.setUser_id(userId);
        template.setJrxmlPath(jrxmlPath.toString());

        return templateRepository.save(template);
    }


    public List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }

    public Template getTemplateById(Long id) throws Exception {
        return templateRepository.findById(id)
                .orElseThrow(() -> new Exception("Template not found"));
    }
}