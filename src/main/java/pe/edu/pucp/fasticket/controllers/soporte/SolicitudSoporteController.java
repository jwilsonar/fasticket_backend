package pe.edu.pucp.fasticket.controllers.soporte;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarEstadoSolicitudDTO;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.CrearSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.SolicitudSoporteResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;
import pe.edu.pucp.fasticket.services.soporte.SolicitudSoporteService;

@RestController
@RequestMapping("/api/v1/soporte")
@Tag(name = "Soporte", description = "Gestión de tickets de soporte y trazabilidad por auditoría")
@RequiredArgsConstructor
@Slf4j
public class SolicitudSoporteController {

    private final SolicitudSoporteService soporteService;

    @Operation(summary = "Crear ticket de soporte", description = "Disponible para clientes autenticados y operadores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket registrado", content = @Content(schema = @Schema(implementation = SolicitudSoporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<StandardResponse<SolicitudSoporteResponseDTO>> crear(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CrearSolicitudSoporteDTO dto) {

        log.info("POST /api/v1/soporte - asunto {}", dto.getAsunto());
        SolicitudSoporteResponseDTO response = soporteService.crear(dto, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success("Ticket de soporte creado", response));
    }

    @Operation(summary = "Listar tickets", description = "Permite filtrar por usuario y estado.")
    @GetMapping
    public ResponseEntity<StandardResponse<List<SolicitudSoporteResponseDTO>>> listar(
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) EstadoSoporte estado) {

        log.info("GET /api/v1/soporte - filtros usuario {} estado {}", idUsuario, estado);
        List<SolicitudSoporteResponseDTO> registros = soporteService.listar(idUsuario, estado);
        return ResponseEntity.ok(StandardResponse.success("Listado de tickets", registros));
    }

    @Operation(summary = "Obtener ticket por ID")
    @ApiResponse(responseCode = "200", description = "Ticket encontrado", content = @Content(schema = @Schema(implementation = SolicitudSoporteResponseDTO.class)))
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<SolicitudSoporteResponseDTO>> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/v1/soporte/{}", id);
        SolicitudSoporteResponseDTO registro = soporteService.obtenerPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Detalle del ticket", registro));
    }

    @Operation(summary = "Actualizar contenido del ticket")
    @PutMapping("/{id}")
    public ResponseEntity<StandardResponse<SolicitudSoporteResponseDTO>> actualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ActualizarSolicitudSoporteDTO dto) {

        log.info("PUT /api/v1/soporte/{}", id);
        SolicitudSoporteResponseDTO actualizado = soporteService.actualizar(id, dto, userDetails);
        return ResponseEntity.ok(StandardResponse.success("Ticket actualizado", actualizado));
    }

    @Operation(summary = "Actualizar estado del ticket")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<StandardResponse<SolicitudSoporteResponseDTO>> actualizarEstado(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ActualizarEstadoSolicitudDTO dto) {

        log.info("PATCH /api/v1/soporte/{}/estado -> {}", id, dto.getEstado());
        SolicitudSoporteResponseDTO actualizado = soporteService.actualizarEstado(id, dto, userDetails);
        return ResponseEntity.ok(StandardResponse.success("Estado actualizado", actualizado));
    }

    @Operation(summary = "Eliminar (lógico) un ticket")
    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DELETE /api/v1/soporte/{}", id);
        soporteService.eliminar(id, userDetails);
        return ResponseEntity.ok(StandardResponse.success("Ticket eliminado lógicamente", null));
    }
}

