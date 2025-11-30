// RUTA: pe.edu.pucp.fasticket.controllers.eventos.EventoController.java

package pe.edu.pucp.fasticket.controllers.eventos;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
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
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listar(
            @Parameter(description = "Mostrar solo activos")
            @RequestParam(defaultValue = "true") boolean soloActivos) {

        log.info("GET /api/v1/eventos?soloActivos={}", soloActivos);
        List<EventoResponseDTO> eventos = soloActivos
                ? eventoService.listarActivos()
                : eventoService.listarTodos();
        StandardResponse<List<EventoResponseDTO>> response = StandardResponse.success("Eventos obtenidos exitosamente", eventos);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar eventos próximos",
            description = "Obtiene eventos futuros ordenados por fecha. Endpoint público."
    )
    @ApiResponse(responseCode = "200", description = "Eventos próximos")
    @GetMapping("/proximos")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarProximos() {
        log.info("GET /api/v1/eventos/proximos");
        List<EventoResponseDTO> eventos = eventoService.listarProximos();
        StandardResponse<List<EventoResponseDTO>> response = StandardResponse.success("Eventos próximos obtenidos exitosamente", eventos);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar eventos por estado",
            description = "Filtra eventos por su estado (ACTIVO, CANCELADO, FINALIZADO)"
    )
    @ApiResponse(responseCode = "200", description = "Eventos filtrados")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarPorEstado(
            @Parameter(description = "Estado del evento", example = "ACTIVO")
            @PathVariable EstadoEvento estado) {

        log.info("GET /api/v1/eventos/estado/{}", estado);
        List<EventoResponseDTO> eventos = eventoService.listarPorEstado(estado);
        StandardResponse<List<EventoResponseDTO>> response = StandardResponse.success("Eventos filtrados por estado obtenidos exitosamente", eventos);
        return ResponseEntity.ok(response);
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
        StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento obtenido exitosamente", evento);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<StandardResponse<EventoResponseDTO>> crear(@Valid @RequestBody EventoCreateDTO dto) {
        log.info("POST /api/v1/eventos - Crear: {}", dto.getNombre());
        EventoResponseDTO evento = eventoService.crear(dto);
        StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento creado exitosamente", evento);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Se eliminan todos los @RequestParam individuales a favor de @ModelAttribute
    @PostMapping(value = "/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<EventoResponseDTO>> crearConImagen(
            @ModelAttribute EventoCreateDTO dto) {

        log.info("POST /api/v1/eventos/con-imagen - Crear: {}", dto.getNombre() != null ? dto.getNombre() : "con imagen");

        try {
            // **Validación de datos**
            if (dto.getNombre() == null || dto.getFechaEvento() == null || dto.getIdLocal() == null || dto.getHoraInicio() == null || dto.getFechaFinEvento() == null) {
                return ResponseEntity.badRequest()
                        .body(StandardResponse.error("Datos del evento incompletos (Nombre, Fecha, Fecha Fin, Hora Inicio y Local son requeridos)."));
            }
            
            // **La lógica de mapeo manual se ha eliminado**
            // Spring ya ha poblado: dto.getNombre(), dto.getFechaEvento(), dto.getImagenUrl(), etc.
            
            // **Delegar al servicio** (El servicio maneja la creación y la subida de archivos)
            EventoResponseDTO evento = eventoService.crear(dto);

            StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento creado exitosamente", evento);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (BusinessException e) {
            log.error("Error de negocio al crear evento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(StandardResponse.error("Error de negocio: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
             log.error("Recurso no encontrado al crear evento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(StandardResponse.error("Recurso no encontrado: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al crear evento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StandardResponse.error("Error interno al crear evento: " + e.getMessage()));
        }
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
    public ResponseEntity<StandardResponse<EventoResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EventoCreateDTO dto) {

        log.info("PUT /api/v1/eventos/{}", id);
        EventoResponseDTO evento = eventoService.actualizar(id, dto);
        StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento actualizado exitosamente", evento);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}/con-imagen", consumes = "multipart/form-data")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<StandardResponse<EventoResponseDTO>> actualizarConImagen(
        @PathVariable Integer id,
        // **CAMBIO CLAVE:** Usamos @ModelAttribute para recibir todos los datos, incluyendo archivos.
        @ModelAttribute EventoCreateDTO dto) { 

    log.info("PUT /api/v1/eventos/{}/con-imagen", id);

    try {
        // La conversión de String a LocalDate/LocalTime/Enum DEBE hacerse aquí si el DTO 
        // no maneja automáticamente esos tipos desde el String del formulario, o bien, 
        // deben estar declarados como String en el DTO y convertidos en el Service.
        
        // Asumiendo que EventoCreateDTO ya maneja las conversiones o los tipos de datos correctos,
        // simplemente llamamos al servicio pasando el ID y el DTO completo.

        // **Delegamos la actualización y el manejo de imágenes al servicio**
        EventoResponseDTO evento = eventoService.actualizarConImagen(id, dto);

        // **Se elimina toda la lógica manual de s3Service y actualizarImagenUrl/ZonasUrl**

        StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento actualizado exitosamente", evento);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("Error al actualizar evento: {}", e.getMessage());
        // Manejo de excepciones más específico si es posible (ResourceNotFound, BusinessException)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al actualizar evento: " + e.getMessage()));
    }
}

    @Operation(
            summary = "Desactivar evento",
            description = "Eliminación lógica del evento. Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "204", description = "Evento desactivado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/eventos/{}", id);
        eventoService.eliminarLogico(id);
        StandardResponse<String> response = StandardResponse.success("Evento eliminado exitosamente");
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtener detalle de evento para proceso de compra",
            description = "Devuelve los datos del evento, su local y los tipos de ticket disponibles. Endpoint público."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}/detalle-compra")
    public ResponseEntity<?> obtenerDetalleEventoParaCompra(
            @Parameter(description = "ID del evento", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("GET /api/v1/eventos/{}/detalle-compra", id);

        try {
            var detalle = eventoService.obtenerDetalleParaCompra(id);
            return ResponseEntity.ok().body(
                    java.util.Map.of(
                            "success", true,
                            "mensajeAviso", "Detalle de evento obtenido exitosamente",
                            "data", detalle
                    )
            );
        } catch (RuntimeException ex) {
            log.error("Error al obtener detalle de evento {}: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    java.util.Map.of(
                            "success", false,
                            "message", "Evento no encontrado"
                    )
            );
        }
    }
    
    @GetMapping("/populares/{topN}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> listarTopEventosPopulares(
            @Parameter(description = "Cantidad de top eventos a devolver", example = "5")
            @PathVariable("topN") @Min(1) @Max(50) Integer topN) {
        
        log.info("GET /api/v1/eventos/populares/{}", topN);
        
        try {
            List<EventoResponseDTO> topEventos = eventoService.listarTopEventosPopulares(topN);
            
            StandardResponse<List<EventoResponseDTO>> response = StandardResponse.success(
                "Top " + topN + " eventos populares obtenidos exitosamente", 
                topEventos
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener eventos populares: {}", e.getMessage(), e);
            
            StandardResponse<List<EventoResponseDTO>> response = StandardResponse.error(
                "Error interno del servidor al obtener eventos populares"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(
            summary = "Obtener ventas totales por evento",
            description = "Devuelve el total de ingresos asociados a un evento (suma de ventas de tickets). Endpoint público."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventas calculadas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/ventas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Double>> obtenerVentasPorEvento(
            @Parameter(description = "ID del evento", required = true, example = "1")
            @PathVariable("id") Integer idEvento) {
        log.info("GET /api/v1/eventos/{}/ventas", idEvento);
        Double total = eventoService.ventasPorEvento(idEvento);
        StandardResponse<Double> response = StandardResponse.success("Ventas totales obtenidas exitosamente", total);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtener ventas totales ",
            description = "Devuelve el total de ingresos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventas calculadas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/ventas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Double>> obtenerVentasTodosEventos() {
        log.info("GET /api/v1/eventos/ventas");
        Double total = eventoService.ventasPorTodosEventos();
        StandardResponse<Double> response = StandardResponse.success("Ventas totales obtenidas exitosamente", total);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Descargar Reporte de Ventas en PDF",
            description = "RF-034: Genera y devuelve un archivo PDF con el resumen de ventas del evento. Solo Admins.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generado", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "500", description = "Error al generar PDF")
    })
    @GetMapping(value = "/{idEvento}/reporte/ventas/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Resource> descargarReporteVentasPdf(
            @Parameter(description = "ID del evento", required = true)
            @PathVariable Integer idEvento) {

        log.info("GET /api/v1/eventos/{}/reporte/ventas/pdf", idEvento);
        try {
            byte[] pdfBytes = eventoService.generarReporteVentasPdf(idEvento);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            // Nombre del archivo PDF
            String filename = "reporte-ventas-evento-" + idEvento + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (IOException e) {
            log.error("Error generando PDF para evento {}: {}", idEvento, e.getMessage(), e);
            // Considera devolver un DTO de error estándar aquí
            return ResponseEntity.internalServerError().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Intento de generar reporte para evento no encontrado ID: {}", idEvento);
            return ResponseEntity.notFound().build();
        }
    } 
}

