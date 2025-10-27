package pe.edu.pucp.fasticket.controllers.eventos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.services.eventos.ZonaServicio;

@Tag(
    name = "Zonas",
    description = "API para gestión de zonas dentro de los locales"
)
@RestController
@RequestMapping("/api/v1/zonas")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class ZonaController {

    private final ZonaServicio zonaServicio;

    @Operation(summary = "Listar todas las zonas")
    @ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public ResponseEntity<StandardResponse<List<Zona>>> listar() {
        log.info("GET /api/v1/zonas");
        List<Zona> zonas = zonaServicio.listarTodas();
        StandardResponse<List<Zona>> response = StandardResponse.success("Zonas obtenidas exitosamente", zonas);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener zona por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zona encontrada"),
        @ApiResponse(responseCode = "404", description = "Zona no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<Zona>> obtenerPorId(
            @Parameter(description = "ID de la zona")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/zonas/{}", id);
        return zonaServicio.buscarPorId(id)
                .map(zona -> {
                    StandardResponse<Zona> response = StandardResponse.success("Zona obtenida exitosamente", zona);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Crear zona",
        description = "Crea una nueva zona. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "201", description = "Zona creada")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Zona>> crear(@Valid @RequestBody Zona zona) {
        log.info("POST /api/v1/zonas - Nombre: {}", zona.getNombre());
        Zona nuevaZona = zonaServicio.crear(zona);
        StandardResponse<Zona> response = StandardResponse.success("Zona creada exitosamente", nuevaZona);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Actualizar zona",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Zona>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody Zona zona) {
        
        log.info("PUT /api/v1/zonas/{}", id);
        zona.setIdZona(id);
        Zona actualizada = zonaServicio.actualizar(zona);
        StandardResponse<Zona> response = StandardResponse.success("Zona actualizada exitosamente", actualizada);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Eliminar zona",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/zonas/{}", id);
        zonaServicio.eliminar(id);
        StandardResponse<String> response = StandardResponse.success("Zona eliminada exitosamente");
        return ResponseEntity.ok(response);
    }
}

