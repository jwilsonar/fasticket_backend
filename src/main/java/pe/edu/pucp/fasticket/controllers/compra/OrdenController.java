package pe.edu.pucp.fasticket.controllers.compra;

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
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

@Tag(
    name = "Órdenes de Compra",
    description = "API para gestión de órdenes de compra. Requiere autenticación."
)
@RestController
@RequestMapping("/api/v1/ordenes")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class OrdenController {

    private final OrdenServicio ordenServicio;

    @Operation(
        summary = "Crear orden de compra",
        description = "Crea una orden de compra desde el carrito del cliente",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Orden creada exitosamente",
            content = @Content(schema = @Schema(implementation = OrdenCompra.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping("/crear")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<OrdenCompra> crearOrden(@Valid @RequestBody CrearOrdenDTO dto) {
        log.info("POST /api/v1/ordenes/crear - Cliente: {}", dto.getIdCliente());
        OrdenCompra orden = ordenServicio.crearOrden(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }

    @Operation(
        summary = "Generar resumen de orden",
        description = "Genera un resumen previo antes de confirmar la compra",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Resumen generado",
        content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
    )
    @PostMapping("/resumen")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<OrdenResumenDTO> generarResumen(@Valid @RequestBody CrearOrdenDTO dto) {
        log.info("POST /api/v1/ordenes/resumen - Cliente: {}", dto.getIdCliente());
        OrdenResumenDTO resumen = ordenServicio.generarResumenOrden(dto);
        return ResponseEntity.ok(resumen);
    }

    @Operation(
        summary = "Confirmar pago de orden",
        description = "Marca una orden como pagada",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Pago confirmado")
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<Void> confirmarPago(
            @Parameter(description = "ID de la orden")
            @PathVariable Integer id) {
        
        log.info("PUT /api/v1/ordenes/{}/confirmar", id);
        ordenServicio.confirmarPagoOrden(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Cancelar orden",
        description = "Cancela una orden de compra",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Orden cancelada")
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<Void> cancelarOrden(
            @Parameter(description = "ID de la orden")
            @PathVariable Integer id) {
        
        log.info("PUT /api/v1/ordenes/{}/cancelar", id);
        ordenServicio.cancelarOrden(id);
        return ResponseEntity.ok().build();
    }
}
