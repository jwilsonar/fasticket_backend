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
        description = "Obtiene lista de todos los tipos de ticket"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public ResponseEntity<List<TipoTicket>> listar() {
        log.info("GET /api/v1/tipos-ticket");
        List<TipoTicket> tiposTicket = tipoTicketServicio.ListarTiposTicket();
        return ResponseEntity.ok(tiposTicket);
    }

    @Operation(
        summary = "Obtener tipo de ticket por ID",
        description = "Obtiene información de un tipo de ticket específico"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tipo de ticket encontrado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tipo de ticket no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipoTicket> obtenerPorId(
            @Parameter(description = "ID del tipo de ticket", required = true)
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/tipos-ticket/{}", id);
        return tipoTicketServicio.BuscarId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Crear tipo de ticket",
        description = "Crea un nuevo tipo de ticket para un evento. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tipo de ticket creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TipoTicket> crear(@Valid @RequestBody TipoTicket tipoTicket) {
        log.info("POST /api/v1/tipos-ticket - Nombre: {}", tipoTicket.getNombre());
        TipoTicket nuevoTipoTicket = tipoTicketServicio.Guardar(tipoTicket);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTipoTicket);
    }

    @Operation(
        summary = "Actualizar tipo de ticket",
        description = "Actualiza un tipo de ticket existente. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de ticket actualizado"),
        @ApiResponse(responseCode = "404", description = "Tipo de ticket no encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TipoTicket> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody TipoTicket tipoTicket) {
        
        log.info("PUT /api/v1/tipos-ticket/{}", id);
        tipoTicket.setIdTipoTicket(id);
        TipoTicket actualizado = tipoTicketServicio.Guardar(tipoTicket);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(
        summary = "Eliminar tipo de ticket",
        description = "Elimina un tipo de ticket. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "204", description = "Tipo de ticket eliminado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/tipos-ticket/{}", id);
        tipoTicketServicio.Eliminar(id);
        return ResponseEntity.noContent().build();
    }
}


