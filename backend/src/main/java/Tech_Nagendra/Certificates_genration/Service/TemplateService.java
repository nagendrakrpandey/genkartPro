package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Entity.TemplateImage;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateImageRepository templateImageRepository;

    public TemplateService(TemplateRepository templateRepository,
                           TemplateImageRepository templateImageRepository) {
        this.templateRepository = templateRepository;
        this.templateImageRepository = templateImageRepository;
    }

    public Template saveTemplate(String templateName,
                                 Integer imageType,
                                 Long userId,
                                 MultipartFile jrxml,
                                 MultipartFile[] images) throws IOException {

        // 1️⃣ Create folder for template
        File folder = new File("C:/certificate_storage/templates/" + templateName);
        if (!folder.exists()) folder.mkdirs();

        // 2️⃣ Save JRXML file
        File jrxmlFile = new File(folder, jrxml.getOriginalFilename());
        jrxml.transferTo(jrxmlFile);

        // 3️⃣ Save template entity
        Template template = new Template();
        template.setTemplateName(templateName);
        template.setImageType(imageType);
        template.setUserId(userId);
        template.setTemplateFolder(folder.getAbsolutePath());
        template.setJrxmlPath(jrxmlFile.getAbsolutePath());

        Template savedTemplate = templateRepository.save(template);

        // 4️⃣ Save images as TemplateImage entities
        if (images != null) {
            for (MultipartFile image : images) {
                if (image.isEmpty()) continue;

                File imgFile = new File(folder, image.getOriginalFilename());
                image.transferTo(imgFile);

                TemplateImage ti = new TemplateImage();
                ti.setTemplate(savedTemplate);          // associate template
                ti.setImagePath(imgFile.getAbsolutePath()); // full path
                ti.setImageType(imageType);               // optional
                templateImageRepository.save(ti);
            }
        }

        return savedTemplate;
    }

    public Template getTemplateById(Long id) throws Exception {
        return templateRepository.findById(id)
                .orElseThrow(() -> new Exception("Template not found with ID: " + id));
    }

    public java.util.List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }
}

