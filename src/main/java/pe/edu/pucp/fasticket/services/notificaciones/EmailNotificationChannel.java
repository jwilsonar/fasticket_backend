package pe.edu.pucp.fasticket.services.notificaciones;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.model.notificaciones.PlantillaNotificacion;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

	private final pe.edu.pucp.fasticket.services.notificaciones.EmailService emailService; // Brevo impl
	private final PlantillaService plantillaService;

	@Override
	public void send(NotificationRequest req) {
		if (!req.isSendEmail()) {
			log.debug("Email channel: sendEmail=false, omitiendo envío para {}", req.getEmail());
			return;
		}
		
		if (req.getEmail() == null || req.getEmail().isBlank()) {
			log.warn("Email channel: email vacío o null, no se puede enviar");
			return;
		}
		
		try {
			log.debug("📧 EmailNotificationChannel: Preparando envío a {}", req.getEmail());
			String subject = req.getSubject();
			String html = req.getHtml();
			
			if (req.getPlantilla() != null) {
				PlantillaNotificacion p = plantillaService.obtenerActiva(req.getPlantilla());
				if (p != null) {
					subject = p.getAsunto();
					html = plantillaService.render(p.getHtml(), req.safeParams());
					log.debug("📄 Usando plantilla activa: {}", req.getPlantilla());
				} else {
					log.debug("📄 Plantilla {} no encontrada o inactiva, usando subject/html del request", req.getPlantilla());
				}
			}
			
			if (subject == null || subject.isBlank()) {
				subject = "Notificación Fasticket";
				log.warn("⚠️ Subject vacío, usando valor por defecto");
			}
			if (html == null || html.isBlank()) {
				html = "<p>No hay contenido HTML disponible.</p>";
				log.warn("⚠️ HTML vacío, usando valor por defecto");
			}
			
			log.info("📤 Enviando email a {} con asunto: {}", req.getEmail(), subject);
			boolean ok = emailService.enviarEmailHtml(req.getEmail(), req.getNombre(), subject, html);
			
			if (!ok) {
				log.error("❌ Email channel: envío NO confirmado para {}. El servicio retornó false. Esto activará el fallback.", req.getEmail());
				// Lanzamos excepción para que el caller pueda usar el fallback
				throw new RuntimeException("El servicio de email (Brevo) retornó false para: " + req.getEmail() + ". Se intentará fallback a SMTP.");
			} else {
				log.info("✅ Email channel: envío confirmado exitosamente para {}", req.getEmail());
			}
		} catch (Exception ex) {
			log.error("❌ Email channel error enviando a {}: {}", req.getEmail(), ex.getMessage(), ex);
			// Re-lanzamos la excepción para que el caller pueda manejarla
			throw new RuntimeException("Error al enviar email a " + req.getEmail() + ": " + ex.getMessage(), ex);
		}
	}
}


