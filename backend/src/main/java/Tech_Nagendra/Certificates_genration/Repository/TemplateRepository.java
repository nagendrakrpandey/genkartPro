package Tech_Nagendra.Certificates_genration.Repository;


import Tech_Nagendra.Certificates_genration.Entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
//   List<Template> findByUserId(Long userId);
//   boolean existsByTemplateName(String templateName);

}
