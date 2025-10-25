package pe.edu.pucp.fasticket.controllers.compra; // Or controllers.ordenes

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

@Tag(
        name = "Órdenes de Compra",
        description = "API para la creación y consulta de órdenes de compra."
)
@RestController
@RequestMapping("/api/v1/ordenes")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class OrdenController {

    private final OrdenServicio ordenServicio;
    private final OrdenCompraRepositorio ordenCompraRepositorio;

    @Operation(
            summary = "Crear nueva orden (Checkout directo)",
            description = "Crea una orden PENDIENTE y reserva tickets. Requiere rol CLIENTE.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> crearOrden(@Valid @RequestBody CrearOrdenDTO crearOrdenDTO) {
        log.info("POST /api/v1/ordenes - Cliente: {}", crearOrdenDTO.getIdCliente());
        OrdenCompra nuevaOrden = ordenServicio.crearOrden(crearOrdenDTO);
        OrdenResumenDTO resumenDTO = new OrdenResumenDTO(nuevaOrden);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StandardResponse.success("Proceso iniciado correctamente.", resumenDTO));
    }
    @Operation(
            summary = "Obtener resumen de una orden creada",
            description = "Obtiene los detalles de una orden ya existente.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen obtenido", content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> obtenerDetalleDeOrden(
            @Parameter(description = "ID de la orden", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("GET /api/v1/ordenes/{}", id);
        OrdenCompra orden = ordenCompraRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));
        OrdenResumenDTO resumen = new OrdenResumenDTO(orden);
        return ResponseEntity.ok(StandardResponse.success("Proceso iniciado correctamente.", resumen));
    }
}