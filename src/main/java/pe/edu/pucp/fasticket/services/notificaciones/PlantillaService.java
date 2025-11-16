package pe.edu.pucp.fasticket.services.notificaciones;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.pucp.fasticket.model.notificaciones.PlantillaNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla;
import pe.edu.pucp.fasticket.repository.notificaciones.PlantillaNotificacionRepositorio;

@Service
@RequiredArgsConstructor
public class PlantillaService {

	private final PlantillaNotificacionRepositorio plantillaRepo;

	private static final Pattern PLACEHOLDER_DOLLAR = Pattern.compile("\\$\\{([a-zA-Z0-9_\\.\\-]+)}");
	private static final Pattern PLACEHOLDER_BRACES = Pattern.compile("\\{\\{([a-zA-Z0-9_\\.\\-]+)}}");

	@Transactional(readOnly = true)
	public PlantillaNotificacion obtenerActiva(TipoPlantilla tipo) {
		return plantillaRepo.findByTipoAndHabilitadoTrue(tipo).orElse(null);
	}

	@Transactional
	public PlantillaNotificacion actualizar(Long id, String asunto, String html, Boolean habilitado) {
		PlantillaNotificacion p = plantillaRepo.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + id));
		if (asunto != null) p.setAsunto(asunto);
		if (html != null) p.setHtml(html);
		if (habilitado != null) p.setHabilitado(habilitado);
		p.setActualizadoEn(LocalDateTime.now());
		return plantillaRepo.save(p);
	}

	public String render(String template, Map<String, Object> params) {
		if (template == null) return "";
		if (params == null || params.isEmpty()) return template;
		// primero ${...}
		String rendered = replaceWithPattern(template, params, PLACEHOLDER_DOLLAR);
		// luego {{...}}
		rendered = replaceWithPattern(rendered, params, PLACEHOLDER_BRACES);
		return rendered;
	}

	private String replaceWithPattern(String template, Map<String, Object> params, Pattern pattern) {
		Matcher m = pattern.matcher(template);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String key = m.group(1);
			Object value = params.getOrDefault(key, "");
			String val = value == null ? "" : String.valueOf(value);
			m.appendReplacement(sb, Matcher.quoteReplacement(val));
		}
		m.appendTail(sb);
		return sb.toString();
	}
}


