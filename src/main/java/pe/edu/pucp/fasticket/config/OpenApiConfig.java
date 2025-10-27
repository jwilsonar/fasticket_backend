package pe.edu.pucp.fasticket.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuración de OpenAPI 3.0 (Swagger) para la documentación de la API REST.
 * 
 * <p>Esta clase configura la interfaz Swagger UI con información del proyecto,
 * servidores disponibles, esquemas de seguridad y metadatos generales.</p>
 * 
 * @author Equipo Fasticket
 * @version 1.0
 * @since 2025-10-10
 */
@Configuration
public class OpenApiConfig {

    @Value("${fasticket.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${fasticket.openapi.prod-url:http://fasticket-alb-prod-2050944455.us-east-1.elb.amazonaws.com}")
    private String prodUrl;

    @Value("${spring.application.name:Fasticket API}")
    private String applicationName;

    /**
     * Configura y personaliza la documentación OpenAPI.
     * 
     * @return Instancia configurada de OpenAPI
     */
    @Bean
    public OpenAPI fasticketOpenAPI() {
        // Servidor de desarrollo
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Servidor de Desarrollo Local");

        // Servidor de producción
        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Servidor de Producción");

        // Información de contacto del equipo
        Contact contact = new Contact();
        contact.setEmail("contacto@fasticket.com");
        contact.setName("Equipo de Desarrollo Fasticket");
        contact.setUrl("https://www.fasticket.com");

        // Licencia del proyecto
        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        // Información general de la API
        Info info = new Info()
                .title("Fasticket REST API")
                .version("1.0.0")
                .contact(contact)
                .description(
                    "API REST para la gestión integral de venta de tickets para eventos. " +
                    "Incluye módulos de eventos, usuarios, compras, pagos, fidelización y más."
                )
                .termsOfService("https://www.fasticket.com/terminos")
                .license(mitLicense);

        // Esquema de seguridad JWT (para autenticación Bearer Token)
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("Ingrese el token JWT con el prefijo 'Bearer '");

        // Requerimiento de seguridad
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        // Documentación externa
        ExternalDocumentation externalDocs = new ExternalDocumentation()
                .description("Documentación completa del proyecto Fasticket")
                .url("https://github.com/fasticket/fasticket-docs");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .externalDocs(externalDocs)
                .components(new Components()
                    .addSecuritySchemes("Bearer Authentication", securityScheme)
                )
                .addSecurityItem(securityRequirement);
    }
}

