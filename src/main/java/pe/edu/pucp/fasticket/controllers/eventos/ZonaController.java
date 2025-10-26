package pe.edu.pucp.fasticket.controllers.eventos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.services.eventos.ZonaServicio;

import java.util.List;

@Tag(
    name = "Zonas",
    description = "API para gestión de zonas dentro de los locales"
)
@RestController
@RequestMapping("/api/v1/zonas")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/zonas")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class ZonaController {

    private final ZonaServicio zonaServicio;

    @Operation(summary = "Listar todas las zonas")
    @ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public ResponseEntity<List<Zona>> listar() {
        log.info("GET /api/v1/zonas");
        List<Zona> zonas = zonaServicio.ListarZonas();
        return ResponseEntity.ok(zonas);
    }

    @Operation(summary = "Obtener zona por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zona encontrada"),
        @ApiResponse(responseCode = "404", description = "Zona no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Zona> obtenerPorId(
            @Parameter(description = "ID de la zona")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/zonas/{}", id);
        return zonaServicio.BuscarId(id)
                .map(ResponseEntity::ok)
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
    public ResponseEntity<Zona> crear(@Valid @RequestBody Zona zona) {
        log.info("POST /api/v1/zonas - Nombre: {}", zona.getNombre());
        Zona nuevaZona = zonaServicio.Guardar(zona);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaZona);
    }

    @Operation(
        summary = "Actualizar zona",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Zona> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody Zona zona) {
        
        log.info("PUT /api/v1/zonas/{}", id);
        zona.setIdZona(id);
        Zona actualizada = zonaServicio.Guardar(zona);
        return ResponseEntity.ok(actualizada);
    }

    @Operation(
        summary = "Eliminar zona",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/zonas/{}", id);
        zonaServicio.Eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

