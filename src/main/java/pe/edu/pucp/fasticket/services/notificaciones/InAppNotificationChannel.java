package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.model.notificaciones.TipoNotificacion;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationChannel implements NotificationChannel {

	private final NotificacionAppService notiService;
	private final PersonasRepositorio personasRepo;

	@Override
	public void send(NotificationRequest req) {
        if (!req.isSendInApp()) return;
		try {
			Integer personaId = req.getPersonaId();
			if (personaId == null && req.getEmail() != null) {
				personaId = personasRepo.findByEmail(req.getEmail()).map(p -> p.getIdPersona()).orElse(null);
			}
			if (personaId == null) {
				log.debug("InApp channel: no personaId resolvible para email={}", req.getEmail());
				return;
			}
			String titulo = req.getTitulo() != null ? req.getTitulo() : deriveTitle(req.getNotiTipo());
			String mensaje = req.getMensaje() != null ? req.getMensaje() : "";
			Map<String,Object> meta = req.getMetadata();
			notiService.notificar(personaId, req.getNotiTipo(), titulo, mensaje, meta);
		} catch (Exception ex) {
			log.error("InApp channel error para {}: {}", req.getEmail(), ex.getMessage(), ex);
		}
	}

	private String deriveTitle(TipoNotificacion t) {
		if (t == null) return "Notificación";
		return switch (t) {
			case VERIFICACION_CUENTA -> "Verifica tu cuenta";
			case CONFIRMACION_COMPRA -> "Compra confirmada";
			case RECORDATORIO_EVENTO -> "Recordatorio de evento";
			case TRANSFERENCIA_OK -> "Transferencia exitosa";
			case TRANSFERENCIA_FALLIDA -> "Transferencia fallida";
			case RECUPERACION_CONTRASENA -> "Recuperación de contraseña";
			case CAMBIO_CONTRASENA -> "Contraseña actualizada";
			case SISTEMA -> "Notificación";
		};
	}
}


