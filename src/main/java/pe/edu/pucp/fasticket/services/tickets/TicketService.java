package pe.edu.pucp.fasticket.services.tickets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.tickets.TicketCreateDTO;
import pe.edu.pucp.fasticket.dto.tickets.TicketDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.*;
// import pe.edu.pucp.fasticket.model.eventos.TipoEstadoTiket; // <--- LÍNEA ELIMINADA (EL ERROR)
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository;
import pe.edu.pucp.fasticket.mapper.TicketMapper;
import pe.edu.pucp.fasticket.services.S3Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepositorio ticketRepository;
    private final EventosRepositorio eventoRepository;
    private final ZonaRepository zonaRepository;
    private final TicketMapper ticketMapper;
    private final S3Service s3Service;

    @Transactional
    public TicketDTO agregarEntradaAEvento(Integer idEvento, TicketCreateDTO ticketDTO) {
        log.info("Agregando entrada '{}' al evento ID: {}", ticketDTO.getNombre(), idEvento);

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        if (evento.getEstadoEvento() != EstadoEvento.BORRADOR) {
            throw new BusinessException("Solo se pueden agregar entradas a eventos en estado BORRADOR");
        }

        Zona zona = zonaRepository.findById(ticketDTO.getIdZona())
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada con ID: " + ticketDTO.getIdZona()));

        if (ticketDTO.getStock() > zona.getAforoMax()) {
            throw new BusinessException("El stock (" + ticketDTO.getStock() + ") no puede superar el aforo de la zona '" + zona.getNombre() + "' (" + zona.getAforoMax() + ")");
        }

        Ticket nuevoTicket = ticketMapper.toEntity(ticketDTO, evento);
        nuevoTicket.setEstado(EstadoTicket.DISPONIBLE);
        nuevoTicket.setActivo(true);

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        log.info("Ticket creado con ID: {}", ticketGuardado.getIdTicket());
        return ticketMapper.toDTO(ticketGuardado);
    }

    public byte[] generarPdfDeTicket(Integer idTicket, String emailClienteLogueado) throws IOException {
        log.info("Solicitud de PDF para Ticket ID: {} por Cliente: {}", idTicket, emailClienteLogueado);

        Ticket ticket = ticketRepository.findById(idTicket)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con ID: " + idTicket));

        if (ticket.getEstado() != EstadoTicket.VENDIDA) {
            throw new BusinessException("Solo se pueden descargar tickets que hayan sido VENDIDOS.");
        }

        if (ticket.getCliente() == null || !ticket.getCliente().getEmail().equals(emailClienteLogueado)) {
            throw new SecurityException("No tiene permiso para descargar este ticket.");
        }

        return generarPdfDeTicket(ticket);
    }

    public byte[] generarPdfDeTicket(Ticket ticket) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Colores (Paleta Elegante)
            Color colorOscuro = new Color(33, 33, 33);      // Casi negro
            Color colorGris = new Color(100, 100, 100);     // Gris texto
            Color colorAcento = new Color(0, 122, 255);     // Azul moderno (iOS style)
            Color colorFondoClaro = new Color(245, 245, 245); // Gris muy suave para fondo

            // Fuentes
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 40;
            float width = pageWidth - 2 * margin;

            // ==========================================
            // 1. FONDO Y MARCO (Estilo Tarjeta)
            // ==========================================

            // Fondo de la tarjeta
            float cardTopY = pageHeight - 100;
            float cardHeight = 500;

            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.addRect(margin, cardTopY - cardHeight, width, cardHeight);
            contentStream.fill();

            // Borde elegante
            contentStream.setStrokingColor(colorOscuro);
            contentStream.setLineWidth(1.5f);
            contentStream.addRect(margin, cardTopY - cardHeight, width, cardHeight);
            contentStream.stroke();

            // Barra Superior de Color
            contentStream.setNonStrokingColor(colorOscuro);
            contentStream.addRect(margin, cardTopY - 60, width, 60);
            contentStream.fill();

            // ==========================================
            // 2. ENCABEZADO
            // ==========================================
            contentStream.beginText();
            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.setFont(fontBold, 22);
            contentStream.newLineAtOffset(margin + 20, cardTopY - 40);
            contentStream.showText("FASTICKET");
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            String textoTicket = "TICKET DE ACCESO";
            float anchoTexto = fontRegular.getStringWidth(textoTicket) / 1000 * 12;
            contentStream.newLineAtOffset(pageWidth - margin - 20 - anchoTexto, cardTopY - 38);
            contentStream.showText(textoTicket);
            contentStream.endText();

            // ==========================================
            // 3. INFORMACIÓN DEL EVENTO (Destacada)
            // ==========================================
            float y = cardTopY - 100;
            float leftCol = margin + 30;

            // Nombre del Evento (Grande)
            contentStream.beginText();
            contentStream.setNonStrokingColor(colorAcento);
            contentStream.setFont(fontBold, 24);
            contentStream.newLineAtOffset(leftCol, y);
            contentStream.showText(ticket.getEvento().getNombre().toUpperCase());
            contentStream.endText();

            y -= 35;

            // Fecha y Hora
            contentStream.beginText();
            contentStream.setNonStrokingColor(colorOscuro);
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(leftCol, y);
            contentStream.showText(ticket.getEvento().getFechaEvento().toString());
            contentStream.setFont(fontRegular, 14);
            contentStream.showText("  |  " + ticket.getEvento().getHoraInicio());
            contentStream.endText();

            y -= 25;

            // Lugar
            contentStream.beginText();
            contentStream.setNonStrokingColor(colorGris);
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(leftCol, y);
            contentStream.showText(ticket.getEvento().getLocal().getNombre());
            contentStream.endText();

            // Línea divisoria punteada (Simulada)
            y -= 30;
            contentStream.setStrokingColor(Color.LIGHT_GRAY);
            contentStream.setLineWidth(1);
            contentStream.moveTo(leftCol, y);
            contentStream.lineTo(pageWidth - margin - 30, y);
            contentStream.stroke();

            // ==========================================
            // 4. DETALLES DEL ASISTENTE (Grid)
            // ==========================================
            y -= 40;
            float col2 = pageWidth / 2;

            // Columna 1: Asistente
            dibujarCampo(contentStream, fontBold, fontRegular, leftCol, y, "ASISTENTE",
                    ticket.getNombreAsistente() + " " + ticket.getApellidoAsistente(), colorGris, colorOscuro);

            // Columna 2: Documento
            dibujarCampo(contentStream, fontBold, fontRegular, col2, y, "DOCUMENTO",
                    ticket.getDocumentoAsistente(), colorGris, colorOscuro);

            y -= 50;

            // Columna 1: Tipo de Entrada
            dibujarCampo(contentStream, fontBold, fontRegular, leftCol, y, "TIPO DE ENTRADA",
                    ticket.getTipoTicket().getNombre(), colorGris, colorOscuro);

            // Columna 2: Zona
            dibujarCampo(contentStream, fontBold, fontRegular, col2, y, "ZONA / UBICACIÓN",
                    ticket.getTipoTicket().getZona().getNombre(), colorGris, colorOscuro);

            // Descargar QR desde S3 si existe la URL
            if (ticket.getQrImageUrl() != null && !ticket.getQrImageUrl().isEmpty()) {
                try {
                    byte[] qrBytes = s3Service.downloadFile(ticket.getQrImageUrl());
                    if (qrBytes != null && qrBytes.length > 0) {
                        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrBytes, "QR");

                        float qrSize = 140;
                        float qrX = (pageWidth - qrSize) / 2;
                        float qrY = cardTopY - cardHeight + 40;
                        contentStream.drawImage(pdImage, qrX, qrY, qrSize, qrSize);
                        contentStream.beginText();
                        contentStream.setNonStrokingColor(colorGris);
                        contentStream.setFont(fontRegular, 8);
                        String codigoTexto = "ID: " + ticket.getCodigoQr();
                        float textWidth = fontRegular.getStringWidth(codigoTexto) / 1000 * 8;
                        contentStream.newLineAtOffset((pageWidth - textWidth) / 2, qrY - 15);
                        contentStream.showText(codigoTexto);
                        contentStream.endText();
                        log.info("QR descargado desde S3 y agregado al PDF del ticket ID: {}", ticket.getIdTicket());
                    } else {
                        log.warn("⚠️ QR descargado desde S3 está vacío para ticket ID: {}", ticket.getIdTicket());
                    }
                } catch (Exception e) {
                    log.error("❌ Error al descargar QR desde S3 para ticket ID: {}: {}", ticket.getIdTicket(), e.getMessage(), e);
                }
            } else {
                log.warn("⚠️ No se encontró URL del QR para ticket ID: {}", ticket.getIdTicket());
            }

            contentStream.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    // Método auxiliar para dibujar pares "Título - Valor" limpios
    private void dibujarCampo(PDPageContentStream stream, PDFont fontLabel, PDFont fontValue,
                              float x, float y, String label, String value, Color cLabel, Color cValue) throws IOException {
        stream.beginText();
        stream.setNonStrokingColor(cLabel);
        stream.setFont(fontLabel, 9);
        stream.newLineAtOffset(x, y);
        stream.showText(label);
        stream.endText();

        stream.beginText();
        stream.setNonStrokingColor(cValue);
        stream.setFont(fontValue, 12);
        stream.newLineAtOffset(x, y - 15);
        stream.showText(value.toUpperCase());
        stream.endText();
    }
}