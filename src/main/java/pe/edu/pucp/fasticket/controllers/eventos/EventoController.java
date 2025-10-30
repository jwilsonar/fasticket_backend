// RUTA: pe.edu.pucp.fasticket.controllers.eventos.EventoController.java

package pe.edu.pucp.fasticket.controllers.eventos;

import java.time.LocalDate;
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
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.services.S3Service;
import pe.edu.pucp.fasticket.services.eventos.EventoService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;

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
    private final S3Service s3Service;

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

    @PostMapping(value = "/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<EventoResponseDTO>> crearConImagen(
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "fechaEvento", required = false) String fechaEvento,
            @RequestParam(value = "horaInicio", required = false) String horaInicio,
            @RequestParam(value = "horaFin", required = false) String horaFin,
            @RequestParam(value = "tipoEvento", required = false) String tipoEvento,
            @RequestParam(value = "estadoEvento", required = false) String estadoEvento,
            @RequestParam(value = "aforoDisponible", required = false) Integer aforoDisponible,
            @RequestParam(value = "idLocal", required = false) Integer idLocal) {

        log.info("POST /api/v1/eventos/con-imagen - Crear: {}", nombre != null ? nombre : "con imagen");

        try {
            if (nombre == null) {
                return ResponseEntity.badRequest()
                        .body(StandardResponse.error("Se requiere información del evento"));
            }

            EventoCreateDTO dto = new EventoCreateDTO();
            dto.setNombre(nombre);
            dto.setDescripcion(descripcion);
            if (fechaEvento != null) {
                dto.setFechaEvento(LocalDate.parse(fechaEvento));
            }
            if (horaInicio != null) {
                dto.setHoraInicio(java.time.LocalTime.parse(horaInicio));
            }
            if (horaFin != null) {
                dto.setHoraFin(java.time.LocalTime.parse(horaFin));
            }
            if (tipoEvento != null) {
                dto.setTipoEvento(TipoEvento.valueOf(tipoEvento));
            }
            if (estadoEvento != null) {
                dto.setEstadoEvento(EstadoEvento.valueOf(estadoEvento));
            }
            dto.setAforoDisponible(aforoDisponible);
            dto.setIdLocal(idLocal);

            EventoResponseDTO evento = eventoService.crear(dto);

            // Subir imagen si se proporcionó
            if (imagen != null && !imagen.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imagen, "eventos", evento.getIdEvento());
                evento.setImagenUrl(imageUrl);
            }

            StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento creado exitosamente", evento);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear evento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(StandardResponse.error("Error al crear evento: " + e.getMessage()));
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
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "fechaEvento", required = false) String fechaEvento,
            @RequestParam(value = "horaInicio", required = false) String horaInicio,
            @RequestParam(value = "horaFin", required = false) String horaFin,
            @RequestParam(value = "tipoEvento", required = false) String tipoEvento,
            @RequestParam(value = "estadoEvento", required = false) String estadoEvento,
            @RequestParam(value = "aforoDisponible", required = false) Integer aforoDisponible,
            @RequestParam(value = "idLocal", required = false) Integer idLocal) {

        log.info("PUT /api/v1/eventos/{}/con-imagen", id);

        try {
            if (nombre == null) {
                return ResponseEntity.badRequest()
                        .body(StandardResponse.error("Se requiere información del evento"));
            }

            EventoCreateDTO dto = new EventoCreateDTO();
            dto.setNombre(nombre);
            dto.setDescripcion(descripcion);
            if (fechaEvento != null) {
                dto.setFechaEvento(LocalDate.parse(fechaEvento));
            }
            if (horaInicio != null) {
                dto.setHoraInicio(java.time.LocalTime.parse(horaInicio));
            }
            if (horaFin != null) {
                dto.setHoraFin(java.time.LocalTime.parse(horaFin));
            }
            if (tipoEvento != null) {
                dto.setTipoEvento(TipoEvento.valueOf(tipoEvento));
            }
            if (estadoEvento != null) {
                dto.setEstadoEvento(EstadoEvento.valueOf(estadoEvento));
            }
            dto.setAforoDisponible(aforoDisponible);
            dto.setIdLocal(idLocal);

            EventoResponseDTO evento = eventoService.actualizar(id, dto);

            // Subir imagen si se proporcionó
            if (imagen != null && !imagen.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imagen, "eventos", evento.getIdEvento());
                evento.setImagenUrl(imageUrl);
            }

            StandardResponse<EventoResponseDTO> response = StandardResponse.success("Evento actualizado exitosamente", evento);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al actualizar evento: {}", e.getMessage());
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

    @Operation(
            summary = "Subir imagen de evento",
            description = "Sube una imagen para un evento específico. Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Imagen subida exitosamente",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Archivo inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    @PostMapping("/{id}/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirImagenEvento(
            @Parameter(description = "ID del evento", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Archivo de imagen", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("POST /api/v1/eventos/{}/imagen", id);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(StandardResponse.error("El archivo no puede estar vacío"));
        }

        try {
            String imageUrl = s3Service.uploadFile(file, "eventos", id);
            StandardResponse<String> response = StandardResponse.success("Imagen subida exitosamente", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imagen del evento {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StandardResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
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

