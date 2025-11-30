package pe.edu.pucp.fasticket.controllers.notificaciones;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import pe.edu.pucp.fasticket.model.notificaciones.PlantillaNotificacion;
import pe.edu.pucp.fasticket.repository.notificaciones.PlantillaNotificacionRepositorio;
import pe.edu.pucp.fasticket.services.notificaciones.PlantillaService;

@Tag(
        name = "Plantillas",
        description = "API para manejar las plantillas"
)
@RestController
@RequestMapping("/plantillas")
@RequiredArgsConstructor
public class PlantillaController {

	private final PlantillaNotificacionRepositorio plantillaRepo;
	private final PlantillaService plantillaService;

	@GetMapping
	public List<PlantillaNotificacion> listar() {
		return plantillaRepo.findAll();
	}

	@PutMapping("/{id}")
	public ResponseEntity<PlantillaNotificacion> actualizar(@PathVariable Long id, @RequestBody UpdatePlantillaRequest body) {
		PlantillaNotificacion p = plantillaService.actualizar(id, body.getAsunto(), body.getHtml(), body.getHabilitado());
		return ResponseEntity.ok(p);
	}

	@Data
	public static class UpdatePlantillaRequest {
		private String asunto;
		private String html;
		private Boolean habilitado;
	}
}


