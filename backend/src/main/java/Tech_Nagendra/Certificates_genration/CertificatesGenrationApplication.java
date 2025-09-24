package Tech_Nagendra.Certificates_genration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class    CertificatesGenrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertificatesGenrationApplication.class, args);
    }

}

