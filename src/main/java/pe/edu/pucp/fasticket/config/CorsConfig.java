package pe.edu.pucp.fasticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS para permitir solicitudes desde orígenes específicos.
 * 
 * <p>Esta configuración permite solicitudes CORS desde:
 * <ul>
 *   <li>El sitio de producción en S3: http://fasticket.s3-website-us-east-1.amazonaws.com</li>
 *   <li>El entorno de desarrollo local: http://localhost:4200</li>
 * </ul>
 * </p>
 * 
 * @author Equipo Fasticket
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "http://fasticket.s3-website-us-east-1.amazonaws.com",
                        "http://localhost:4200" // para desarrollo
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }
}

