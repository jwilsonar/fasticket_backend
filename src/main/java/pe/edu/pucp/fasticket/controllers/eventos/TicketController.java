package pe.edu.pucp.fasticket.controllers.eventos;

// Imports de Spring y Lombok
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// Imports de Swagger
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

// Imports de tu proyecto
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.compra.TransferenciaResponseDTO;
import pe.edu.pucp.fasticket.services.compra.TransferenciaEntradaServicio;
import pe.edu.pucp.fasticket.services.tickets.TicketService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tickets", description = "Endpoints para gestionar tickets individuales (incluyendo historial)")
@SecurityRequirement(name = "Bearer Authentication")
public class TicketController {

    private final TicketService ticketService;
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

    @Operation(
            summary = "Descargar ticket en PDF",
            description = "RF-092: Permite al cliente autenticado descargar su propio ticket (que esté VENDIDA) en formato PDF.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "PDF generado",
            content = @Content(mediaType = "application/pdf"))
    @GetMapping("/{id}/descargar-pdf")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<byte[]> descargarTicketPdf(
            @Parameter(description = "ID del ticket individual a descargar")
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            byte[] pdfBytes = ticketService.generarPdfDeTicket(id, userDetails.getUsername());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            // Esto fuerza al navegador a descargar el archivo
            String filename = "ticket-" + id + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Error al generar el stream del PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}