package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

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

    private final TransactionalEmailsApi transactionalEmailsApi;

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.sender-email:noreply@fasticket.com}")
    private String senderEmail;

    @Value("${brevo.sender-name:Fasticket}")
    private String senderName;

    @Value("${brevo.enabled:false}")
    private boolean enabled;

    public BrevoEmailService(TransactionalEmailsApi transactionalEmailsApi) {
        this.transactionalEmailsApi = transactionalEmailsApi;
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
			final String cid = UUID.randomUUID().toString();
			final long startNs = System.nanoTime();

			log.debug("BREVO [{}] -> Preparando envío con plantilla | enabled={} | senderEmail={} | to={} | asunto={} | templateId={} | paramKeys={}",
				cid, enabled, senderEmail, maskEmail(destinatario), safe(asunto), templateId,
				parametros != null ? parametros.keySet() : java.util.Collections.emptySet());

			if (apiKey == null || apiKey.isBlank()) {
				log.error("❌ BREVO [{}] API key vacía o no configurada. Revisa BREVO_API_KEY / brevo.api-key", cid);
				return false;
			}

            SendSmtpEmail email = new SendSmtpEmail()
                .sender(new SendSmtpEmailSender().email(senderEmail).name(senderName))
                .to(java.util.Collections.singletonList(
                    new SendSmtpEmailTo().email(destinatario).name(nombreDestinatario)
                ))
                .subject(asunto)
                .templateId(templateId)
                .params(sanitizeParams(parametros));

			var response = transactionalEmailsApi.sendTransacEmail(email);

			long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
			log.info("✅ BREVO [{}] Email (plantilla) enviado | to={} | asunto={} | templateId={} | elapsedMs={} | messageId={}",
				cid, maskEmail(destinatario), safe(asunto), templateId, elapsedMs,
				response != null ? response.getMessageId() : "n/a");

            return true;

        } catch (Exception e) {
            // CRÍTICO: Capturamos la excepción para que no rompa el flujo principal
			log.error("❌ BREVO Error enviando email (plantilla) a {}: {}", maskEmail(destinatario), e.getMessage(), e);
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
			final String cid = UUID.randomUUID().toString();
			final long startNs = System.nanoTime();

			log.debug("BREVO [{}] -> Preparando envío HTML | enabled={} | senderEmail={} | to={} | asunto={} | htmlSize={}",
				cid, enabled, senderEmail, maskEmail(destinatario), safe(asunto),
				contenidoHtml != null ? contenidoHtml.length() : 0);

			if (apiKey == null || apiKey.isBlank()) {
				log.error("❌ BREVO [{}] API key vacía o no configurada. Revisa BREVO_API_KEY / brevo.api-key", cid);
				return false;
			}

            SendSmtpEmail email = new SendSmtpEmail()
                .sender(new SendSmtpEmailSender().email(senderEmail).name(senderName))
                .to(java.util.Collections.singletonList(
                    new SendSmtpEmailTo().email(destinatario).name(nombreDestinatario)
                ))
                .subject(asunto)
                .htmlContent(contenidoHtml);

			var response = transactionalEmailsApi.sendTransacEmail(email);

			long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
			log.info("✅ BREVO [{}] Email HTML enviado | to={} | asunto={} | htmlSize={} | elapsedMs={} | messageId={}",
				cid, maskEmail(destinatario), safe(asunto),
				contenidoHtml != null ? contenidoHtml.length() : 0, elapsedMs,
				response != null ? response.getMessageId() : "n/a");
            return true;

        } catch (Exception e) {
			log.error("❌ BREVO Error enviando email HTML a {}: {}", maskEmail(destinatario), e.getMessage(), e);
            return false;
        }
    }

	private Map<String, Object> sanitizeParams(Map<String, Object> src) {
		Map<String, Object> out = new HashMap<>();
		if (src == null) return out;
		src.forEach((k, v) -> out.put(k, v == null ? "" : v));
		return out;
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) return "hidden";
		String[] parts = email.split("@", 2);
		String local = parts[0];
		String domain = parts[1];
		String maskedLocal = local.length() <= 2 ? "**" : local.substring(0, 2) + "***";
		return maskedLocal + "@" + domain;
	}

	private String safe(String s) {
		if (s == null) return "";
		return s.replaceAll("[\\r\\n]", " ").trim();
	}
}

