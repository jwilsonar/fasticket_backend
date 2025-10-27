package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de email usando Brevo (SendinBlue).
 * 
 * Esta clase maneja toda la interacción con la API de Brevo de forma robusta,
 * garantizando que los errores no afecten el flujo principal de la aplicación.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Service
@Slf4j
public class BrevoEmailService implements EmailService {

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    @Value("${brevo.sender-email:noreply@fasticket.com}")
    private String senderEmail;

    @Value("${brevo.sender-name:Fasticket}")
    private String senderName;

    @Value("${brevo.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public BrevoEmailService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean enviarEmail(String destinatario, String nombreDestinatario, String asunto,
                               Long templateId, Map<String, Object> parametros) {
        
        // Si Brevo no está habilitado, solo loguear
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Brevo no está habilitado. Email simulado enviado a: {} con template: {}", 
                     destinatario, templateId);
            log.info("📧 [SIMULADO] Asunto: {} | Destinatario: {} | Parámetros: {}", 
                     asunto, destinatario, parametros);
            return true;
        }

        try {
            log.info("📧 Enviando email a {} con plantilla ID: {}", destinatario, templateId);

            HttpHeaders headers = crearHeaders();
            Map<String, Object> requestBody = construirRequestConTemplate(
                destinatario, nombreDestinatario, asunto, templateId, parametros
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email enviado exitosamente a: {}", destinatario);
                return true;
            } else {
                log.error("❌ Error al enviar email. Status: {} | Response: {}", 
                         response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            // CRÍTICO: Capturamos la excepción para que no rompa el flujo principal
            log.error("❌ Excepción al enviar email a {}: {}", destinatario, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean enviarEmailHtml(String destinatario, String nombreDestinatario,
                                   String asunto, String contenidoHtml) {
        
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Brevo no está habilitado. Email HTML simulado enviado a: {}", destinatario);
            log.info("📧 [SIMULADO] Asunto: {} | Destinatario: {}", asunto, destinatario);
            return true;
        }

        try {
            log.info("📧 Enviando email HTML a {}", destinatario);

            HttpHeaders headers = crearHeaders();
            Map<String, Object> requestBody = construirRequestConHtml(
                destinatario, nombreDestinatario, asunto, contenidoHtml
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email HTML enviado exitosamente a: {}", destinatario);
                return true;
            } else {
                log.error("❌ Error al enviar email HTML. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Excepción al enviar email HTML a {}: {}", destinatario, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Crea los headers necesarios para la API de Brevo.
     */
    private HttpHeaders crearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * Construye el cuerpo de la petición usando una plantilla de Brevo.
     */
    private Map<String, Object> construirRequestConTemplate(String destinatario, 
                                                            String nombreDestinatario,
                                                            String asunto, 
                                                            Long templateId,
                                                            Map<String, Object> parametros) {
        Map<String, Object> body = new HashMap<>();
        
        // Remitente
        Map<String, String> sender = new HashMap<>();
        sender.put("email", senderEmail);
        sender.put("name", senderName);
        body.put("sender", sender);

        // Destinatarios
        Map<String, String> to = new HashMap<>();
        to.put("email", destinatario);
        to.put("name", nombreDestinatario);
        body.put("to", List.of(to));

        // Template y parámetros
        body.put("templateId", templateId);
        body.put("params", parametros != null ? parametros : new HashMap<>());
        body.put("subject", asunto);

        return body;
    }

    /**
     * Construye el cuerpo de la petición con HTML personalizado.
     */
    private Map<String, Object> construirRequestConHtml(String destinatario,
                                                        String nombreDestinatario,
                                                        String asunto,
                                                        String contenidoHtml) {
        Map<String, Object> body = new HashMap<>();
        
        Map<String, String> sender = new HashMap<>();
        sender.put("email", senderEmail);
        sender.put("name", senderName);
        body.put("sender", sender);

        Map<String, String> to = new HashMap<>();
        to.put("email", destinatario);
        to.put("name", nombreDestinatario);
        body.put("to", List.of(to));

        body.put("subject", asunto);
        body.put("htmlContent", contenidoHtml);

        return body;
    }
}

