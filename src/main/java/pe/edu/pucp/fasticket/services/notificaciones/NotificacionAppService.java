package pe.edu.pucp.fasticket.services.notificaciones;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import pe.edu.pucp.fasticket.model.notificaciones.NotificacionUsuario;
import pe.edu.pucp.fasticket.model.notificaciones.PreferenciasNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoNotificacion;
import pe.edu.pucp.fasticket.repository.notificaciones.NotificacionUsuarioRepositorio;
import pe.edu.pucp.fasticket.repository.notificaciones.PreferenciasNotificacionRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificacionAppService {

	private final NotificacionUsuarioRepositorio notiRepo;
	private final PreferenciasNotificacionRepositorio prefRepo;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Transactional
	public NotificacionUsuario notificar(Integer personaId, TipoNotificacion tipo, String titulo, String mensaje, Map<String, Object> metadata) {
		// respetar preferencia (por defecto habilitado si no existe)
		boolean habilitado = prefRepo.findById(personaId).map(PreferenciasNotificacion::isHabilitado).orElse(true);
		if (!habilitado) {
			return null;
		}
		NotificacionUsuario n = new NotificacionUsuario();
		n.setPersonaId(personaId);
		n.setTipo(tipo != null ? tipo : TipoNotificacion.SISTEMA);
		n.setTitulo(StringUtils.hasText(titulo) ? titulo : (tipo != null ? tipo.name() : "Notificación"));
		n.setMensaje(mensaje != null ? mensaje : "");
		n.setCreadaEn(Instant.now());
		n.setLeida(false);
		if (metadata != null && !metadata.isEmpty()) {
			try {
				n.setMetadataJson(objectMapper.writeValueAsString(metadata));
			} catch (JsonProcessingException ignored) { }
		}
		return notiRepo.save(n);
	}

	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<NotificacionUsuario> listar(Integer personaId, boolean soloNoLeidas, org.springframework.data.domain.Pageable pageable) {
		return soloNoLeidas
			? notiRepo.findByPersonaIdAndLeidaOrderByCreadaEnDesc(personaId, false, pageable)
			: notiRepo.findByPersonaIdOrderByCreadaEnDesc(personaId, pageable);
	}

	@Transactional
	public void marcarLeidas(Integer personaId, List<Long> ids) {
		if (ids == null || ids.isEmpty()) return;
		List<NotificacionUsuario> list = notiRepo.findAllById(ids);
		Instant now = Instant.now();
		for (NotificacionUsuario n : list) {
			if (n.getPersonaId().equals(personaId)) {
				n.setLeida(true);
				n.setLeidaEn(now);
			}
		}
		notiRepo.saveAll(list);
	}

	@Transactional(readOnly = true)
	public boolean obtenerHabilitado(Integer personaId) {
		return prefRepo.findById(personaId).map(PreferenciasNotificacion::isHabilitado).orElse(true);
	}

	@Transactional
	public PreferenciasNotificacion actualizarPreferencia(Integer personaId, boolean habilitado) {
		PreferenciasNotificacion pref = prefRepo.findById(personaId).orElseGet(() -> {
			PreferenciasNotificacion p = new  PreferenciasNotificacion();
			p.setPersonaId(personaId);
			return p;
		});
		pref.setHabilitado(habilitado);
		return prefRepo.save(pref);
	}
}


