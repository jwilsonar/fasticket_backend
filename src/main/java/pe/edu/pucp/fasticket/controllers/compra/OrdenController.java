package pe.edu.pucp.fasticket.controllers.compra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
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
        description = "Crea una orden de compra desde el carrito del cliente y reserva los tickets",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Orden creada exitosamente",
            content = @Content(schema = @Schema(implementation = OrdenCompra.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o carrito vacío"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @PostMapping("/crear")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenCompra>> crearOrden(@Valid @RequestBody CrearOrdenDTO dto) {
        log.info("POST /api/v1/ordenes/crear - Cliente: {}", dto.getIdCliente());
        OrdenCompra orden = ordenServicio.crearOrden(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success("Orden creada exitosamente", orden));
    }

    @Operation(
        summary = "Generar resumen de orden",
        description = "Genera un resumen previo con el total y detalles antes de confirmar la compra",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resumen generado exitosamente",
            content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        )
    })
    @PostMapping("/resumen")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> generarResumen(@Valid @RequestBody CrearOrdenDTO dto) {
        log.info("POST /api/v1/ordenes/resumen - Cliente: {}", dto.getIdCliente());
        OrdenResumenDTO resumen = ordenServicio.generarResumenOrden(dto);
        return ResponseEntity.ok(StandardResponse.success("Resumen de orden generado exitosamente", resumen));
    }

    @Operation(
        summary = "Confirmar pago de orden",
        description = "Marca una orden como pagada y activa los tickets",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pago confirmado exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        )
    })
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> confirmarPago(
            @Parameter(description = "ID de la orden", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("PUT /api/v1/ordenes/{}/confirmar", id);
        ordenServicio.confirmarPagoOrden(id);
        return ResponseEntity.ok(StandardResponse.success("Pago confirmado exitosamente"));
    }

    @Operation(
        summary = "Cancelar orden",
        description = "Cancela una orden de compra y libera los tickets reservados",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orden cancelada exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        )
    })
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> cancelarOrden(
            @Parameter(description = "ID de la orden", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("PUT /api/v1/ordenes/{}/cancelar", id);
        ordenServicio.cancelarOrden(id);
        return ResponseEntity.ok(StandardResponse.success("Orden cancelada exitosamente"));
    }

    @Operation(
        summary = "Anular compra",
        description = "RF-089: Permite al administrador anular una compra dentro de las reglas definidas",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Compra anulada exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos (requiere rol ADMINISTRADOR)"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "La orden no se puede anular (estado inválido)"
        )
    })
    @PutMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> anularCompra(
            @Parameter(description = "ID de la orden a anular", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("PUT /api/v1/ordenes/{}/anular - Anulación por administrador", id);
        ordenServicio.anularCompra(id);
        return ResponseEntity.ok(StandardResponse.success("Compra anulada exitosamente. Los cupos han sido liberados."));
    }
}
