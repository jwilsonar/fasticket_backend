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
import org.springframework.web.bind.annotation.RequestParam;
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
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.services.eventos.EventoService;

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
public class EventoController {

    private final EventoService eventoService;

    @Operation(
        summary = "Listar todos los eventos",
        description = "Obtiene lista completa de eventos activos o todos según el parámetro. Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista obtenida exitosamente"
    )
    @GetMapping
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listar(
            @Parameter(description = "Mostrar solo eventos activos", example = "true")
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        
        log.info("GET /api/v1/eventos?soloActivos={}", soloActivos);
        List<EventoResponseDTO> eventos = soloActivos 
            ? eventoService.listarActivos() 
            : eventoService.listarTodos();
        return ResponseEntity.ok(StandardResponse.success("Lista de eventos obtenida exitosamente", eventos));
    }

    @Operation(
        summary = "Listar eventos próximos",
        description = "Obtiene eventos futuros ordenados por fecha de inicio. Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Eventos próximos obtenidos exitosamente"
    )
    @GetMapping("/proximos")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarProximos() {
        log.info("GET /api/v1/eventos/proximos");
        List<EventoResponseDTO> eventos = eventoService.listarProximos();
        return ResponseEntity.ok(StandardResponse.success("Eventos próximos obtenidos exitosamente", eventos));
    }

    @Operation(
        summary = "Listar eventos por estado",
        description = "Filtra eventos por su estado (ACTIVO, CANCELADO, FINALIZADO). Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Eventos filtrados exitosamente"
    )
    @GetMapping("/estado/{estado}")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarPorEstado(
            @Parameter(
                description = "Estado del evento", 
                required = true,
                example = "ACTIVO"
            )
            @PathVariable EstadoEvento estado) {
        
        log.info("GET /api/v1/eventos/estado/{}", estado);
        List<EventoResponseDTO> eventos = eventoService.listarPorEstado(estado);
        return ResponseEntity.ok(StandardResponse.success("Eventos filtrados por estado obtenidos exitosamente", eventos));
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
    public ResponseEntity<StandardResponse<EventoResponseDTO>> obtenerPorId(
            @Parameter(description = "ID del evento", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/eventos/{}", id);
        EventoResponseDTO evento = eventoService.obtenerPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Evento obtenido exitosamente", evento));
    }

    @Operation(
        summary = "Crear nuevo evento",
        description = "Crea un nuevo evento con sus fechas, local y configuración. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Evento creado exitosamente",
            content = @Content(schema = @Schema(implementation = EventoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o fechas incorrectas"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos (requiere rol ADMINISTRADOR)"
        )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<EventoResponseDTO>> crear(@Valid @RequestBody EventoCreateDTO dto) {
        log.info("POST /api/v1/eventos - Crear: {}", dto.getNombre());
        EventoResponseDTO response = eventoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success("Evento creado exitosamente", response));
    }

    @Operation(
        summary = "Actualizar evento",
        description = "Actualiza información de un evento existente. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Evento actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = EventoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Evento no encontrado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<EventoResponseDTO>> actualizar(
            @Parameter(description = "ID del evento a actualizar", required = true, example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody EventoCreateDTO dto) {
        
        log.info("PUT /api/v1/eventos/{}", id);
        EventoResponseDTO response = eventoService.actualizar(id, dto);
        return ResponseEntity.ok(StandardResponse.success("Evento actualizado exitosamente", response));
    }

    @Operation(
        summary = "Desactivar evento",
        description = "Eliminación lógica del evento (no se elimina físicamente). Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Evento desactivado exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Evento no encontrado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> eliminar(
            @Parameter(description = "ID del evento a desactivar", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("DELETE /api/v1/eventos/{}", id);
        eventoService.eliminarLogico(id);
        return ResponseEntity.ok(StandardResponse.success("Evento desactivado exitosamente"));
    }

    @Operation(
        summary = "Filtrar eventos por tipo/categoría",
        description = "RF-065: Filtra eventos por tipo (CONCIERTO, TEATRO, DEPORTIVO, etc.). Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Eventos filtrados exitosamente"
    )
    @GetMapping("/tipo/{tipoEvento}")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarPorTipo(
            @Parameter(description = "Tipo de evento", required = true, example = "CONCIERTO")
            @PathVariable String tipoEvento) {
        
        log.info("GET /api/v1/eventos/tipo/{}", tipoEvento);
        List<EventoResponseDTO> eventos = eventoService.listarPorTipo(tipoEvento);
        return ResponseEntity.ok(StandardResponse.success("Eventos filtrados por tipo exitosamente", eventos));
    }

    @Operation(
        summary = "Filtrar eventos por rango de fechas",
        description = "RF-066: Filtra eventos por rango de fechas. Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Eventos encontrados en el rango"
    )
    @GetMapping("/fechas")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarPorRangoFechas(
            @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd)", required = true, example = "2025-01-01")
            @RequestParam String fechaInicio,
            @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd)", required = true, example = "2025-12-31")
            @RequestParam String fechaFin) {
        
        log.info("GET /api/v1/eventos/fechas?fechaInicio={}&fechaFin={}", fechaInicio, fechaFin);
        java.time.LocalDate inicio = java.time.LocalDate.parse(fechaInicio);
        java.time.LocalDate fin = java.time.LocalDate.parse(fechaFin);
        List<EventoResponseDTO> eventos = eventoService.listarPorRangoFechas(inicio, fin);
        return ResponseEntity.ok(StandardResponse.success("Eventos filtrados por fecha exitosamente", eventos));
    }

    @Operation(
        summary = "Filtrar eventos por distrito",
        description = "RF-067: Filtra eventos por ubicación (distrito del local). Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Eventos encontrados en el distrito"
    )
    @GetMapping("/distrito/{idDistrito}")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarPorDistrito(
            @Parameter(description = "ID del distrito", required = true, example = "1")
            @PathVariable Integer idDistrito) {
        
        log.info("GET /api/v1/eventos/distrito/{}", idDistrito);
        List<EventoResponseDTO> eventos = eventoService.listarPorDistrito(idDistrito);
        return ResponseEntity.ok(StandardResponse.success("Eventos filtrados por distrito exitosamente", eventos));
    }

    @Operation(
        summary = "Listar eventos ordenados por fecha",
        description = "RF-069: Obtiene eventos ordenados por fecha de inicio ascendente. Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Eventos ordenados exitosamente"
    )
    @GetMapping("/ordenados")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarOrdenadosPorFecha() {
        log.info("GET /api/v1/eventos/ordenados");
        List<EventoResponseDTO> eventos = eventoService.listarOrdenadosPorFecha();
        return ResponseEntity.ok(StandardResponse.success("Eventos ordenados por fecha exitosamente", eventos));
    }

    @Operation(
        summary = "Cancelar evento",
        description = "RF-016: Cancela un evento y genera acciones de comunicación. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Evento cancelado exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Evento no encontrado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> cancelarEvento(
            @Parameter(description = "ID del evento a cancelar", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("PUT /api/v1/eventos/{}/cancelar", id);
        eventoService.cancelarEvento(id);
        return ResponseEntity.ok(StandardResponse.success("Evento cancelado exitosamente"));
    }
}

