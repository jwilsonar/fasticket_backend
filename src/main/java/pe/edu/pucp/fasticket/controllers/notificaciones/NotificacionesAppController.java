package pe.edu.pucp.fasticket.controllers.notificaciones;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.model.notificaciones.NotificacionUsuario;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.notificaciones.NotificacionAppService;

@Tag(
        name = "Notificaciones",
        description = "API para controlar la App de notificaciones"
)
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionesAppController {

	private final NotificacionAppService notiService;
	private final PersonasRepositorio personasRepo;

	@GetMapping
	public StandardResponse<Page<NotificacionUsuario>> listar(@AuthenticationPrincipal UserDetails userDetails,
	                                                          @RequestParam(name = "unread", required = false, defaultValue = "false") boolean unread,
	                                                          Pageable pageable) {
		var persona = personasRepo.findByEmail(userDetails.getUsername()).orElseThrow();
		var page = notiService.listar(persona.getIdPersona(), unread, pageable);
		return StandardResponse.success("OK", page);
	}

	@PostMapping("/marcar-leidas")
	public StandardResponse<String> marcarLeidas(@AuthenticationPrincipal UserDetails userDetails, @RequestBody MarkReadRequest req) {
		var persona = personasRepo.findByEmail(userDetails.getUsername()).orElseThrow();
		notiService.marcarLeidas(persona.getIdPersona(), req.getIds());
		return StandardResponse.success("Notificaciones actualizadas");
	}

	@GetMapping("/preferencias")
	public StandardResponse<Map<String, Object>> obtenerPref(@AuthenticationPrincipal UserDetails userDetails) {
		var persona = personasRepo.findByEmail(userDetails.getUsername()).orElseThrow();
		boolean habilitado = notiService.obtenerHabilitado(persona.getIdPersona());
		return StandardResponse.success("OK", Map.of("habilitado", Boolean.valueOf(habilitado)));
	}

	@PostMapping("/preferencias")
	public StandardResponse<Map<String, Object>> actualizarPref(@AuthenticationPrincipal UserDetails userDetails, @RequestBody PrefRequest req) {
		var persona = personasRepo.findByEmail(userDetails.getUsername()).orElseThrow();
		var saved = notiService.actualizarPreferencia(persona.getIdPersona(), req.isHabilitado());
		return StandardResponse.success("OK", Map.of("habilitado", Boolean.valueOf(saved.isHabilitado())));
	}

	@Data
	public static class MarkReadRequest {
		private List<Long> ids;
	}

	@Data
	public static class PrefRequest {
		private boolean habilitado;
	}
}


