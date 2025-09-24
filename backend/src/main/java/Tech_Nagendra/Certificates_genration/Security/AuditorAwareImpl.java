package Tech_Nagendra.Certificates_genration.Security;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String currentUser  = SecurityContextHolder.getContext().getAuthentication().getName();
        return Optional.of(currentUser != null ? currentUser : "SYSTEM");
    }
}