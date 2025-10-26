// RUTA: pe.edu.pucp.fasticket.controllers.eventos.EventoController.java

package pe.edu.pucp.fasticket.controllers.eventos;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.services.eventos.EventoService;

import java.util.List;

@Tag(
    name = "Eventos",
    description = "API para gestión de eventos. " +
                  "Los endpoints de lectura son públicos, " +
                  "pero crear/modificar requiere rol de administrador."
)
@RestController
@RequestMapping("/api/v1/eventos")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/eventos")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class EventoController {

    private final EventoService eventoService;

    @Operation(
        summary = "Listar todos los eventos",
        description = "Obtiene lista de eventos. Endpoint público."
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> listar(
            @Parameter(description = "Mostrar solo activos")
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        
        log.info("GET /api/v1/eventos?soloActivos={}", soloActivos);
        List<EventoResponseDTO> eventos = soloActivos 
            ? eventoService.listarActivos() 
            : eventoService.listarTodos();
        return ResponseEntity.ok(eventos);
    }

    @Operation(
        summary = "Listar eventos próximos",
        description = "Obtiene eventos futuros ordenados por fecha. Endpoint público."
    )
    @ApiResponse(responseCode = "200", description = "Eventos próximos")
    @GetMapping("/proximos")
    public ResponseEntity<List<EventoResponseDTO>> listarProximos() {
        log.info("GET /api/v1/eventos/proximos");
        List<EventoResponseDTO> eventos = eventoService.listarProximos();
        return ResponseEntity.ok(eventos);
    }

    @Operation(
        summary = "Listar eventos por estado",
        description = "Filtra eventos por su estado (ACTIVO, CANCELADO, FINALIZADO)"
    )
    @ApiResponse(responseCode = "200", description = "Eventos filtrados")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EventoResponseDTO>> listarPorEstado(
            @Parameter(description = "Estado del evento", example = "ACTIVO")
            @PathVariable EstadoEvento estado) {
        
        log.info("GET /api/v1/eventos/estado/{}", estado);
        List<EventoResponseDTO> eventos = eventoService.listarPorEstado(estado);
        return ResponseEntity.ok(eventos);
    }

    @Operation(
        summary = "Obtener evento por ID",
        description = "Obtiene información detallada de un evento. Endpoint público."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Evento encontrado",
            content = @Content(schema = @Schema(implementation = EventoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerPorId(
            @Parameter(description = "ID del evento", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/eventos/{}", id);
        EventoResponseDTO evento = eventoService.obtenerPorId(id);
        return ResponseEntity.ok(evento);
    }

    @Operation(
        summary = "Crear nuevo evento",
        description = "Crea un evento. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Evento creado",
            content = @Content(schema = @Schema(implementation = EventoResponseDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EventoResponseDTO> crear(@Valid @RequestBody EventoCreateDTO dto) {
        log.info("POST /api/v1/eventos - Crear: {}", dto.getNombre());
        EventoResponseDTO response = eventoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Actualizar evento",
        description = "Actualiza un evento existente. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento actualizado"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EventoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EventoCreateDTO dto) {
        
        log.info("PUT /api/v1/eventos/{}", id);
        EventoResponseDTO response = eventoService.actualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Desactivar evento",
        description = "Eliminación lógica del evento. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "204", description = "Evento desactivado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/eventos/{}", id);
        eventoService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }
}

