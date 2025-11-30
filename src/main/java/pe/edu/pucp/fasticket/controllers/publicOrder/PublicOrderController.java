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
import pe.edu.pucp.fasticket.repository.pago.ComprobanteDePagoRepositorio;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.services.tickets.TicketService;
import pe.edu.pucp.fasticket.services.S3Service;

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
    private final ComprobanteDePagoRepositorio comprobanteDePagoRepositorio;
    private final S3Service s3Service;
    
    @GetMapping("/comprobante/{codigoSeguimiento}")
    public ResponseEntity<byte[]> descargarComprobantePublico(@PathVariable String codigoSeguimiento) {

        // 1. Buscar por el código secreto (UUID), NO por ID
        OrdenCompra orden = ordenRepository.findByCodigoSeguimiento(codigoSeguimiento)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado o enlace inválido"));

        // 2. Validar que tenga pago y comprobante
        if (orden.getPago() == null || orden.getPago().getComprobantePago() == null) {
            return ResponseEntity.notFound().build();
        }

        // 3. Cargar directamente el comprobante desde el repositorio para evitar problemas con relaciones lazy
        pe.edu.pucp.fasticket.model.pago.ComprobantePago comprobante = comprobanteDePagoRepositorio
                .findById(orden.getPago().getComprobantePago().getIdComprobante())
                .orElse(null);
        
        // 4. Validar que tenga URL del PDF en S3
        if (comprobante == null || comprobante.getPdfUrl() == null || comprobante.getPdfUrl().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 5. Descargar el PDF desde S3
        try {
            byte[] pdfContent = s3Service.downloadFile(comprobante.getPdfUrl());
            
            if (pdfContent == null || pdfContent.length == 0) {
                log.error("PDF vacío descargado desde S3 para código {}", codigoSeguimiento);
                return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Validar que el PDF sea válido (debe empezar con %PDF)
            if (pdfContent.length < 4) {
                log.error("PDF corrupto para código {}: tamaño insuficiente ({} bytes)", codigoSeguimiento, pdfContent.length);
                return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            String header = new String(pdfContent, 0, Math.min(4, pdfContent.length));
            if (!header.equals("%PDF")) {
                log.error("PDF corrupto para código {}: no tiene header PDF válido (inicio: {})", codigoSeguimiento, header);
                return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprobante.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            log.error("Error al descargar PDF desde S3 para código {}: {}", codigoSeguimiento, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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