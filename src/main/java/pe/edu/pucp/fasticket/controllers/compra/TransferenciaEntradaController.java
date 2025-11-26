package pe.edu.pucp.fasticket.controllers.compra;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.compra.*;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.fasticket.security.UserDetailsImpl;
import pe.edu.pucp.fasticket.services.compra.TransferenciaEntradaServicio;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transferencias")
@RequiredArgsConstructor
@Tag(name = "Transferencias", description = "Endpoints para gestionar solicitudes y transferencias de tickets")
@SecurityRequirement(name = "Bearer Authentication")
public class TransferenciaEntradaController {

    private final TransferenciaEntradaServicio transferenciaService;

    @Operation(
            summary = "Crear solicitud de transferencia",
            description = "El propietario del ticket envía una solicitud de transferencia al destinatario. " +
                    "Valida límites de transferencias, cooldown y datos del receptor. " +
                    "El receptor tendrá 48 horas para aceptar o rechazar."
    )
    @PostMapping("/solicitudes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<SolicitudTransferenciaDTO>> crearSolicitud(
            @Valid @RequestBody CrearSolicitudTransferenciaDTO requestDTO,
            Authentication authentication) {

        Integer idEmisor = obtenerIdUsuarioLogueado(authentication);
        log.info("Creando solicitud de transferencia de ticket {} por emisor ID: {}",
                requestDTO.getIdTicket(), idEmisor);

        SolicitudTransferenciaDTO resultado = transferenciaService
                .crearSolicitudTransferencia(idEmisor, requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success("Solicitud de transferencia enviada exitosamente", resultado));
    }

    @Operation(
            summary = "Obtener solicitudes recibidas",
            description = "Lista todas las solicitudes de transferencia pendientes que el usuario ha recibido como destinatario. " +
                    "Muestra tiempo restante para responder antes de que expiren."
    )
    @GetMapping("/solicitudes/recibidas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<SolicitudTransferenciaDTO>>> obtenerSolicitudesRecibidas(
            Authentication authentication) {

        Integer idReceptor = obtenerIdUsuarioLogueado(authentication);
        log.info("Obteniendo solicitudes recibidas para usuario ID: {}", idReceptor);

        List<SolicitudTransferenciaDTO> solicitudes = transferenciaService
                .obtenerSolicitudesPendientes(idReceptor);

        return ResponseEntity.ok(
                StandardResponse.success("Solicitudes recibidas obtenidas exitosamente", solicitudes));
    }

    @Operation(
            summary = "Obtener solicitudes enviadas",
            description = "Lista todas las solicitudes de transferencia que el usuario ha enviado como emisor. " +
                    "Incluye solicitudes pendientes, aceptadas, rechazadas, canceladas y expiradas."
    )
    @GetMapping("/solicitudes/enviadas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<SolicitudTransferenciaDTO>>> obtenerSolicitudesEnviadas(
            Authentication authentication) {

        Integer idEmisor = obtenerIdUsuarioLogueado(authentication);
        log.info("Obteniendo solicitudes enviadas por usuario ID: {}", idEmisor);

        List<SolicitudTransferenciaDTO> solicitudes = transferenciaService
                .obtenerSolicitudesEnviadas(idEmisor);

        return ResponseEntity.ok(
                StandardResponse.success("Solicitudes enviadas obtenidas exitosamente", solicitudes));
    }

    @Operation(
            summary = "Responder a solicitud de transferencia",
            description = "El destinatario acepta o rechaza una solicitud de transferencia. " +
                    "Si acepta: la transferencia se ejecuta inmediatamente y el ticket cambia de propietario. " +
                    "Si rechaza: la solicitud se marca como rechazada. " +
                    "Solo válido para solicitudes pendientes dentro del plazo de 48 horas."
    )
    @PostMapping("/solicitudes/{idSolicitud}/responder")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<SolicitudTransferenciaDTO>> responderSolicitud(
            @PathVariable Integer idSolicitud,
            @Valid @RequestBody ResponderSolicitudDTO requestDTO,
            Authentication authentication) {

        Integer idReceptor = obtenerIdUsuarioLogueado(authentication);
        log.info("Respondiendo solicitud {} por receptor ID: {}, acción: {}",
                idSolicitud, idReceptor, requestDTO.getAceptar() ? "ACEPTAR" : "RECHAZAR");

        requestDTO.setIdSolicitud(idSolicitud);

        SolicitudTransferenciaDTO resultado = transferenciaService
                .responderSolicitud(idReceptor, requestDTO);

        String mensaje = requestDTO.getAceptar()
                ? "Solicitud aceptada. La transferencia se ha completado exitosamente."
                : "Solicitud rechazada.";

        return ResponseEntity.ok(StandardResponse.success(mensaje, resultado));
    }

    @Operation(
            summary = "Cancelar solicitud de transferencia",
            description = "El emisor puede cancelar una solicitud pendiente antes de que el receptor responda. " +
                    "Solo válido para solicitudes en estado PENDIENTE."
    )
    @DeleteMapping("/solicitudes/{idSolicitud}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> cancelarSolicitud(
            @PathVariable Integer idSolicitud,
            Authentication authentication) {

        Integer idEmisor = obtenerIdUsuarioLogueado(authentication);
        log.info("Cancelando solicitud {} por emisor ID: {}", idSolicitud, idEmisor);

        transferenciaService.cancelarSolicitud(idEmisor, idSolicitud);

        return ResponseEntity.ok(
                StandardResponse.success("Solicitud cancelada exitosamente", null));
    }

    @Operation(
            summary = "Ver historial de transferencias de un ticket",
            description = "Muestra el historial completo de todas las transferencias ejecutadas para un ticket específico. " +
                    "Incluye emisor, receptor y fecha de cada transferencia."
    )
    @GetMapping("/historial/{idTicket}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<TransferenciaResponseDTO>>> verHistorialTicket(
            @PathVariable Integer idTicket,
            Authentication authentication) {

        Integer idUsuario = obtenerIdUsuarioLogueado(authentication);
        log.info("Obteniendo historial de transferencias del ticket {} por usuario ID: {}",
                idTicket, idUsuario);

        List<TransferenciaResponseDTO> historial = transferenciaService
                .verHistorialDeTicket(idTicket);

        return ResponseEntity.ok(
                StandardResponse.success("Historial de transferencias obtenido exitosamente", historial));
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