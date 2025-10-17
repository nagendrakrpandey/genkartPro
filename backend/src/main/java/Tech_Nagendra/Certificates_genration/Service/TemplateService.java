package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Dto.TemplateDto;
import Tech_Nagendra.Certificates_genration.Entity.Template;
import Tech_Nagendra.Certificates_genration.Entity.TemplateImage;
import Tech_Nagendra.Certificates_genration.Entity.UserProfile;
import Tech_Nagendra.Certificates_genration.Repository.ProfileRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateImageRepository;
import Tech_Nagendra.Certificates_genration.Repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateImageRepository templateImageRepository;
    private final ProfileRepository profileRepository;

    private static final String TEMPLATE_BASE_PATH = "C:/certificate_storage/templates/";

    // Template save करने का method
    public TemplateDto saveTemplate(Long userId,
                                    String templateName,
                                    Integer imageType,
                                    MultipartFile jrxml,
                                    MultipartFile[] images) throws IOException {

        UserProfile currentUser = profileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));

        File folder = new File(TEMPLATE_BASE_PATH + templateName);
        if (!folder.exists()) folder.mkdirs();

        File jrxmlFile = new File(folder, jrxml.getOriginalFilename());
        jrxml.transferTo(jrxmlFile);

        Template template = new Template();
        template.setTemplateName(templateName);
        template.setImageType(imageType);
        template.setTemplateFolder(folder.getAbsolutePath());
        template.setJrxmlPath(jrxmlFile.getAbsolutePath());
        template.setCreatedBy(currentUser);
        template.setModifiedBy(currentUser);
        template.setCreatedAt(LocalDateTime.now());
        template.setModifiedAt(LocalDateTime.now());

        Template savedTemplate = templateRepository.save(template);

        List<TemplateImage> savedImages = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                if (image.isEmpty()) continue;

                File imgFile = new File(folder, image.getOriginalFilename());
                image.transferTo(imgFile);

                TemplateImage ti = new TemplateImage();
                ti.setTemplate(savedTemplate);
                ti.setImagePath(imgFile.getAbsolutePath());
                ti.setImageType(imageType);

                savedImages.add(templateImageRepository.save(ti));
            }
        }
        savedTemplate.setImages(savedImages);

        List<String> imagePaths = savedImages.stream()
                .map(TemplateImage::getImagePath)
                .collect(Collectors.toList());

        return mapToDto(savedTemplate, imagePaths);
    }

    // Single template fetch करने का method
    public TemplateDto getTemplateByIdForUser(Long templateId, Long userId, String role) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Admin को सब template access, normal user सिर्फ अपने-created template
        if (!"ADMIN".equalsIgnoreCase(role) && !template.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("You do not have access to this template");
        }

        List<String> imagePaths = template.getImages().stream()
                .map(TemplateImage::getImagePath)
                .collect(Collectors.toList());

        return mapToDto(template, imagePaths);
    }

    // All templates fetch करने का method
    public List<TemplateDto> getAllTemplates(Long userId, String role) {
        List<Template> templates;

        if ("ADMIN".equalsIgnoreCase(role)) {
            // Admin को सारे templates दिखेंगे
            templates = templateRepository.findAll();
        } else {
            // Normal user को सिर्फ उनके created templates दिखेंगे
            templates = templateRepository.findByCreatedBy_Id(userId);
        }

        return templates.stream()
                .map(template -> mapToDto(template, template.getImages().stream()
                        .map(TemplateImage::getImagePath)
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    // Total templates count (Admin & User wise)
    public Long getTotalTemplates(Long userId, String role) {
        return "ADMIN".equalsIgnoreCase(role)
                ? templateRepository.count()
                : templateRepository.countByCreatedBy_Id(userId);
    }

    private TemplateDto mapToDto(Template template, List<String> imagePaths) {
        return new TemplateDto(
                template.getId(),
                template.getTemplateName(),
                template.getImageType(),
                template.getJrxmlPath(),
                template.getTemplateFolder(),
                template.getCreatedAt(),
                template.getModifiedAt(),
                imagePaths
        );
    }
}
