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
import pe.edu.pucp.fasticket.dto.eventos.ActualizarTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.CrearTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.services.eventos.TipoTicketServicio;

@Tag(
        name = "Tipos de Ticket",
        description = "API para gestión de tipos de tickets para eventos"
)
@RestController
@RequestMapping("/api/v1/tipos-ticket")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class TipoTicketController {

    private final TipoTicketServicio tipoTicketServicio;

    @Operation(
            summary = "Listar tipos de ticket",
            description = "Obtiene lista de todos los tipos de ticket disponibles"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<StandardResponse<List<TipoTicketDTO>>> listar(
            @Parameter(description = "Filtrar por zona específica")
            @RequestParam(required = false) Integer zona) {
        
        log.info("GET /api/v1/tipos-ticket?zona={}", zona);
        List<TipoTicketDTO> tiposTicket = zona != null 
            ? tipoTicketServicio.listarPorZona(zona)
            : tipoTicketServicio.listarTodos();
        return ResponseEntity.ok(StandardResponse.success("Lista de tipos de ticket obtenida exitosamente", tiposTicket));
    }

    @Operation(
            summary = "Obtener tipo de ticket por ID",
            description = "Obtiene información detallada de un tipo de ticket específico"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de ticket encontrado",
                    content = @Content(schema = @Schema(implementation = TipoTicket.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de ticket no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<TipoTicketDTO>> obtenerPorId(
            @Parameter(description = "ID del tipo de ticket", required = true, example = "1")
            @PathVariable Integer id) {
        log.info("GET /api/v1/tipos-ticket/{}", id);
        TipoTicketDTO tipoTicket = tipoTicketServicio.obtenerPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Tipo de ticket obtenido exitosamente", tipoTicket));
    }

    @Operation(
            summary = "Crear tipo de ticket",
            description = "Crea un nuevo tipo de ticket para un evento (ej: VIP, General, Platea). Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tipo de ticket creado exitosamente",
                    content = @Content(schema = @Schema(implementation = TipoTicket.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
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
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<TipoTicketDTO>> crear(@Valid @RequestBody CrearTipoTicketRequestDTO requestDTO) {
        log.info("POST /api/v1/tipos-ticket - Nombre: {} para zona: {}", requestDTO.getNombre(), requestDTO.getIdZona());
        TipoTicketDTO nuevoTipoTicket = tipoTicketServicio.crear(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success("Tipo de ticket creado exitosamente", nuevoTipoTicket));
    }

    @Operation(
            summary = "Actualizar tipo de ticket",
            description = "Actualiza un tipo de ticket existente. Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de ticket actualizado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de ticket no encontrado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos"
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<TipoTicketDTO>> actualizar(
            @Parameter(description = "ID del tipo de ticket a actualizar", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarTipoTicketRequestDTO updateDTO) {

        log.info("PUT /api/v1/tipos-ticket/{}", id);
        TipoTicketDTO actualizado = tipoTicketServicio.actualizar(id, updateDTO);
        return ResponseEntity.ok(StandardResponse.success("Tipo de ticket actualizado exitosamente", actualizado));
    }

    @Operation(
            summary = "Eliminar tipo de ticket",
            description = "Elimina un tipo de ticket del sistema. Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de ticket eliminado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de ticket no encontrado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos"
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> eliminar(
            @Parameter(description = "ID del tipo de ticket a eliminar", required = true)
            @PathVariable Integer id) {

        log.info("DELETE /api/v1/tipos-ticket/{}", id);
        tipoTicketServicio.eliminar(id);
        return ResponseEntity.ok(StandardResponse.success("Tipo de ticket eliminado exitosamente"));
    }

    @Operation(
            summary = "Desactivar tipo de ticket",
            description = "Desactiva un tipo de ticket (eliminación lógica). Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo de ticket desactivado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tipo de ticket no encontrado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos"
            )
    })
    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> desactivar(
            @Parameter(description = "ID del tipo de ticket a desactivar", required = true)
            @PathVariable Integer id) {

        log.info("PUT /api/v1/tipos-ticket/{}/desactivar", id);
        tipoTicketServicio.desactivar(id);
        return ResponseEntity.ok(StandardResponse.success("Tipo de ticket desactivado exitosamente"));
    }
}


