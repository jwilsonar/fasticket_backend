package pe.edu.pucp.fasticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import sibApi.TransactionalEmailsApi;

@Configuration
@Profile("!test") // No cargar esta configuración en el perfil de test
public class BrevoConfig {

    @Bean
    public TransactionalEmailsApi transactionalEmailsApi(@Value("${brevo.api-key:}") String apiKey) {
        // Configura el API Key en el cliente del SDK
        TransactionalEmailsApi api = new TransactionalEmailsApi();
        api.getApiClient().setApiKey(apiKey);
        return api;
    }
}


