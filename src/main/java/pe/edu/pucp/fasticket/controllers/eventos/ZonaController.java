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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(
        summary = "Listar todas las zonas",
        description = "Obtiene lista de todas las zonas disponibles en los locales"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista obtenida exitosamente"
    )
    @GetMapping
    public ResponseEntity<StandardResponse<List<Zona>>> listar() {
        log.info("GET /api/v1/zonas");
        List<Zona> zonas = zonaServicio.ListarZonas();
        return ResponseEntity.ok(StandardResponse.success("Lista de zonas obtenida exitosamente", zonas));
    }

    @Operation(
        summary = "Obtener zona por ID",
        description = "Obtiene información detallada de una zona específica"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Zona encontrada",
            content = @Content(schema = @Schema(implementation = Zona.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zona no encontrada"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<Zona>> obtenerPorId(
            @Parameter(description = "ID de la zona", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/zonas/{}", id);
        return zonaServicio.BuscarId(id)
                .map(zona -> ResponseEntity.ok(StandardResponse.success("Zona obtenida exitosamente", zona)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Crear zona",
        description = "Crea una nueva zona dentro de un local. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Zona creada exitosamente",
            content = @Content(schema = @Schema(implementation = Zona.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos (requiere rol ADMINISTRADOR)"
        )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Zona>> crear(@Valid @RequestBody Zona zona) {
        log.info("POST /api/v1/zonas - Nombre: {}", zona.getNombre());
        Zona nuevaZona = zonaServicio.Guardar(zona);
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success("Zona creada exitosamente", nuevaZona));
    }

    @Operation(
        summary = "Actualizar zona",
        description = "Actualiza información de una zona existente. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Zona actualizada exitosamente",
            content = @Content(schema = @Schema(implementation = Zona.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zona no encontrada"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Zona>> actualizar(
            @Parameter(description = "ID de la zona a actualizar", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody Zona zona) {
        
        log.info("PUT /api/v1/zonas/{}", id);
        zona.setIdZona(id);
        Zona actualizada = zonaServicio.Guardar(zona);
        return ResponseEntity.ok(StandardResponse.success("Zona actualizada exitosamente", actualizada));
    }

    @Operation(
        summary = "Eliminar zona",
        description = "Elimina una zona del sistema. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Zona eliminada exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zona no encontrada"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> eliminar(
            @Parameter(description = "ID de la zona a eliminar", required = true)
            @PathVariable Integer id) {
        
        log.info("DELETE /api/v1/zonas/{}", id);
        zonaServicio.Eliminar(id);
        return ResponseEntity.ok(StandardResponse.success("Zona eliminada exitosamente"));
    }
}

