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
		if (!req.isSendEmail()) return;
		try {
			String subject = req.getSubject();
			String html = req.getHtml();
			if (req.getPlantilla() != null) {
				PlantillaNotificacion p = plantillaService.obtenerActiva(req.getPlantilla());
				if (p != null) {
					subject = p.getAsunto();
					html = plantillaService.render(p.getHtml(), req.safeParams());
				}
			}
			if (subject == null) subject = "";
			if (html == null) html = "";
			boolean ok = emailService.enviarEmailHtml(req.getEmail(), req.getNombre(), subject, html);
			if (!ok) {
				log.warn("Email channel: envío no confirmado para {}", req.getEmail());
			}
		} catch (Exception ex) {
			log.error("Email channel error enviando a {}: {}", req.getEmail(), ex.getMessage(), ex);
		}
	}
}


