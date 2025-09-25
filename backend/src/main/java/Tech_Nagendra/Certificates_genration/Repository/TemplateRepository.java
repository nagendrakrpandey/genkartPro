package Tech_Nagendra.Certificates_genration.Repository;


import Tech_Nagendra.Certificates_genration.Entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
   // Template findByTemplateName(String templateName);
    Optional<Template> findByTemplateName(String templateName);

}
