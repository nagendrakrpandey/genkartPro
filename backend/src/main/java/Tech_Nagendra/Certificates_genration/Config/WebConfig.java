package Tech_Nagendra.Certificates_genration.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Base folder where files are stored
    private static final String BASE_PATH = "file:///C:/certificate_storage/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/templates/**")
                .addResourceLocations(BASE_PATH + "templates/")
                .setCachePeriod(3600); // 1 hour cache


        registry.addResourceHandler("/template-images/**")
                .addResourceLocations(BASE_PATH + "template-images/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/generated/**")
                .addResourceLocations(BASE_PATH + "generated/")
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8081",   // React Dev Server
                        "http://127.0.0.1:8081"   // optional fallback
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition") // allows file download headers
                .allowCredentials(true)
                .maxAge(3600); // cache preflight for 1 hour
    }
}
