package pe.edu.pucp.fasticket.controllers.compra;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.compra.TransferenciaResponseDTO;
import pe.edu.pucp.fasticket.dto.compra.VerificarTransferenciaRequestDTO;
import pe.edu.pucp.fasticket.dto.compra.VerificarTransferenciaResponseDTO;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.fasticket.security.CustomUserDetailsService;
import pe.edu.pucp.fasticket.security.UserDetailsImpl;
import pe.edu.pucp.fasticket.services.compra.TransferenciaEntradaServicio;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/transferencias")
@RequiredArgsConstructor
@Tag(name = "Transferencias", description = "Endpoints para transferir tickets")
@SecurityRequirement(name = "Bearer Authentication")
public class TransferenciaEntradaController {
    private final TransferenciaEntradaServicio transferenciaService;

    @Operation(summary = "Verificar destinatario y reglas de transferencia",
            description = "Valida los datos del destinatario y las reglas de límites/cooldown. " +
                    "Si es exitoso, devuelve los datos para la pantalla de confirmación.")
    @PostMapping("/verificar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<VerificarTransferenciaResponseDTO>> verificarTransferencia(
            @Valid @RequestBody VerificarTransferenciaRequestDTO requestDTO,
            Authentication authentication) {
        Integer idEmisor = obtenerIdUsuarioLogueado(authentication);
        log.info("Verificando transferencia de ticket {} para emisor ID: {}", requestDTO.getIdTicket(), idEmisor);
        VerificarTransferenciaResponseDTO resumen = transferenciaService.verificarTransferencia(idEmisor, requestDTO);
        return ResponseEntity.ok(
                StandardResponse.success("Destinatario verificado. Listo para confirmar.", resumen)
        );
    }

    @Operation(summary = "Ejecutar una transferencia de ticket",
            description = "Ejecuta la transferencia instantánea. (Pantalla 2: Botón Confirmar)")
    @PostMapping("/ejecutar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<TransferenciaResponseDTO>> ejecutarTransferencia(
            @Valid @RequestBody VerificarTransferenciaRequestDTO requestDTO,
            Authentication authentication) {

        Integer idEmisor = obtenerIdUsuarioLogueado(authentication);
        log.info("Ejecutando transferencia de ticket {} para emisor ID: {}", requestDTO.getIdTicket(), idEmisor);
        TransferenciaResponseDTO historial = transferenciaService.ejecutarTransferencia(idEmisor, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success("Transferencia completada exitosamente", historial));
    }

    private Integer obtenerIdUsuarioLogueado(Authentication authentication) {
        if (authentication == null) {
            throw new SecurityException("No se pudo determinar el usuario autenticado (authentication null).");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl)) {
            throw new SecurityException("El principal de autenticación no es de la clase UserDetailsImpl esperada.");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) principal;
        return userDetails.getIdPersona();
    }
}
