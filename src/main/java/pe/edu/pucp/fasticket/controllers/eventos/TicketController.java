package pe.edu.pucp.fasticket.controllers.eventos;

// Imports de Spring y Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Imports de Swagger
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

// Imports de tu proyecto
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.compra.TransferenciaResponseDTO;
import pe.edu.pucp.fasticket.services.compra.TransferenciaEntradaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets") // <--- ESTA ES LA RUTA BASE
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tickets", description = "Endpoints para gestionar tickets individuales (incluyendo historial)")
@SecurityRequirement(name = "Bearer Authentication")
public class TicketController {

    private final TransferenciaEntradaServicio transferenciaService;
    @Operation(summary = "Ver historial de transferencias de un ticket",
            description = "Muestra todos los dueños anteriores de un ticket específico.")
    @GetMapping("/{idTicket}/historial")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<TransferenciaResponseDTO>>> verHistorial(
            @PathVariable Integer idTicket,
            Authentication authentication) {
        log.info("GET /api/v1/tickets/{}/historial", idTicket);
        List<TransferenciaResponseDTO> historial = transferenciaService.verHistorialDeTicket(idTicket);

        return ResponseEntity.ok(StandardResponse.success("Historial de ticket obtenido", historial));
    }
}