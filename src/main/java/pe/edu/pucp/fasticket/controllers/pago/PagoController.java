package pe.edu.pucp.fasticket.controllers.pago;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.pago.ComprobanteDTO;
import pe.edu.pucp.fasticket.dto.pago.RegistrarPagoDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.services.pago.PagoServicio;

@Tag(
    name = "Pagos",
    description = "API para procesamiento de pagos. Requiere autenticación."
)
@RestController
@RequestMapping("/api/v1/pagos")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class PagoController {

    private final PagoServicio pagoServicio;

    @Operation(
        summary = "Registrar pago",
        description = "Procesa el pago de una orden de compra y genera comprobante",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pago procesado exitosamente",
            content = @Content(schema = @Schema(implementation = ComprobanteDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de pago inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping("/registrar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ComprobanteDTO> registrarPago(@Valid @RequestBody RegistrarPagoDTO dto) {
        log.info("POST /api/v1/pagos/registrar - Orden: {}", dto.getIdOrden());
        ComprobanteDTO comprobante = pagoServicio.registrarPagoFinal(dto);
        return ResponseEntity.ok(comprobante);
    }
}

