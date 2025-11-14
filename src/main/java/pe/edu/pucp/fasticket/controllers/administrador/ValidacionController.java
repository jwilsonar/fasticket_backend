package pe.edu.pucp.fasticket.controllers.administrador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.ValidacionResponseDTO;
import pe.edu.pucp.fasticket.services.ValidacionService;

@Tag(
        name = "Validación de Entradas (Admin/Staff)",
        description = "API para validar tickets (RF-094)"
)
@RestController
@RequestMapping("/api/v1/admin/validacion") // Lo ponemos bajo 'admin' para reusar la seguridad
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMINISTRADOR')") // Idealmente sería 'STAFF', pero usamos 'ADMIN' para no crear conflictos
public class ValidacionController {

    private final ValidacionService validacionService;

    @Operation(
            summary = "Validar Ticket por Código QR",
            description = "RF-094: Escanea un código QR (string), valida el ticket y lo marca como 'CANJEADO'. Devuelve los datos del asistente."
    )
    @PostMapping("/qr")
    public ResponseEntity<StandardResponse<ValidacionResponseDTO>> validarQr(
            @Parameter(description = "El string exacto del código QR", required = true)
            @RequestParam String codigoQr) {

        log.info("POST /api/v1/admin/validacion/qr - Código: {}", codigoQr);

        ValidacionResponseDTO respuesta = validacionService.validarTicket(codigoQr);

        return ResponseEntity.ok(StandardResponse.success(respuesta.getMensaje(), respuesta));
    }
}