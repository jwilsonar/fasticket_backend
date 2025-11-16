package pe.edu.pucp.fasticket.services.notificaciones;

import java.util.Collections;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import pe.edu.pucp.fasticket.model.notificaciones.TipoNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla;

@Data
@Builder
public class NotificationRequest {
	private Integer personaId;         // opcional; si null se puede resolver por email
	private String email;              // para canal email
	private String nombre;             // nombre del destinatario (para email y app)

	private TipoNotificacion notiTipo; // tipo lógico para app

	// Contenido para email por plantilla
	private TipoPlantilla plantilla;   // opcional; si no se provee se usa subject/html
	private Map<String, Object> params;
	private String subject;            // fallback para email
	private String html;               // fallback para email

	// Contenido para app
	private String titulo;             // título de la notificación in-app
	private String mensaje;            // contenido de la notificación in-app
	private Map<String, Object> metadata;

	// Canales a utilizar
	@Builder.Default
	private boolean sendEmail = true;
	@Builder.Default
	private boolean sendInApp = true;

	public Map<String, Object> safeParams() {
		return this.params != null ? this.params : Collections.emptyMap();
	}
}


