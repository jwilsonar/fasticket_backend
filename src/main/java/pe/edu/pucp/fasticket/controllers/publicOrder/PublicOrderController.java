package pe.edu.pucp.fasticket.controllers.publicOrder;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.services.tickets.TicketService;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(
        name = "Ordenes (de compra)",
        description = "API auxiliar para manejar órdenes de compra."
)
@Slf4j
@RestController
@RequestMapping("/api/v1/public/ordenes") // Ruta pública
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrdenCompraRepositorio ordenRepository;
    private final TicketService ticketService;
    @GetMapping("/comprobante/{codigoSeguimiento}")
    public ResponseEntity<byte[]> descargarComprobantePublico(@PathVariable String codigoSeguimiento) {

        // 1. Buscar por el código secreto (UUID), NO por ID
        OrdenCompra orden = ordenRepository.findByCodigoSeguimiento(codigoSeguimiento)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado o enlace inválido"));

        // 2. Validar que tenga PDF
        if (orden.getPago() == null || orden.getPago().getComprobantePago() == null
                || orden.getPago().getComprobantePago().getPdfContenido() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfContent = orden.getPago().getComprobantePago().getPdfContenido();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprobante.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/tickets/{codigoSeguimiento}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarTicketsPublico(@PathVariable String codigoSeguimiento) {

        // 1. Buscar la orden por el código secreto
        OrdenCompra orden = ordenRepository.findByCodigoSeguimiento(codigoSeguimiento)
                .orElseThrow(() -> new ResourceNotFoundException("Enlace inválido o expirado"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            boolean tieneTickets = false;

            // 2. Recorrer todos los tickets de la orden
            for (ItemCarrito item : orden.getItems()) {
                for (Ticket ticket : item.getTickets()) {
                    try {
                        // Generar el PDF individual
                        byte[] pdfBytes = ticketService.generarPdfDeTicket(ticket);

                        if (pdfBytes != null) {
                            // Crear entrada en el ZIP
                            String nombreArchivo = "Ticket-" + ticket.getIdTicket() + "-" +
                                    ticket.getNombreAsistente().replace(" ", "_") + ".pdf";

                            ZipEntry entry = new ZipEntry(nombreArchivo);
                            zos.putNextEntry(entry);
                            zos.write(pdfBytes);
                            zos.closeEntry();
                            tieneTickets = true;
                        }
                    } catch (Exception e) {
                        log.error("Error al generar ticket {} para descarga pública: {}", ticket.getIdTicket(), e.getMessage());
                    }
                }
            }

            if (!tieneTickets) {
                return ResponseEntity.notFound().build();
            }

            zos.finish();

            // 3. Retornar el archivo ZIP
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"MisEntradas-Fasticket.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Tipo genérico binario (o application/zip)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            log.error("Error generando ZIP de tickets públicos", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}