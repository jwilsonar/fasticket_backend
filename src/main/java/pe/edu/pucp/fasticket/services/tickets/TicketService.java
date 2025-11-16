package pe.edu.pucp.fasticket.services.tickets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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

        // --- CORRECCIÓN AQUÍ ---
        // Cambiamos el ENUM por el String exacto de tu base de datos
        nuevoTicket.setEstado(EstadoTicket.DISPONIBLE);
        // --- FIN DE LA CORRECCIÓN ---

        nuevoTicket.setActivo(true);

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        log.info("Ticket creado con ID: {}", ticketGuardado.getIdTicket());
        return ticketMapper.toDTO(ticketGuardado);
    }

    public byte[] generarPdfDeTicket(Integer idTicket, String emailClienteLogueado) throws IOException {
        log.info("Generando PDF para Ticket ID: {} por Cliente: {}", idTicket, emailClienteLogueado);
        Ticket ticket = ticketRepository.findById(idTicket)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con ID: " + idTicket));
        if (ticket.getEstado() != EstadoTicket.VENDIDA) {
            throw new BusinessException("Solo se pueden descargar tickets que hayan sido VENDIDOS.");
        }
        if (ticket.getCliente() == null || !ticket.getCliente().getEmail().equals(emailClienteLogueado)) {
            throw new SecurityException("No tiene permiso para descargar este ticket.");
        }
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(150, 750);
            contentStream.showText("Fasticket - Tu Entrada Digital");
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(50, 680);
            contentStream.showText(ticket.getEvento().getNombre());
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Fecha: " + ticket.getEvento().getFechaEvento().toString());
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Lugar: " + ticket.getEvento().getLocal().getNombre());
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(50, 600);
            contentStream.showText("Entrada: " + ticket.getTipoTicket().getNombre());
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Zona: " + ticket.getTipoTicket().getZona().getNombre());
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Precio Pagado: S/ " + String.format("%.2f", ticket.getPrecio()));
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(50, 520);
            contentStream.showText("Asistente:");
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText(ticket.getNombreAsistente() + " " + ticket.getApellidoAsistente());
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText(ticket.getTipoDocumentoAsistente().toString() + ": " + ticket.getDocumentoAsistente());
            contentStream.endText();

            if (ticket.getQrImage() != null) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, ticket.getQrImage(), "QR");
                contentStream.drawImage(pdImage, 200, 250, 200, 200);
            } else {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(200, 350);
                contentStream.showText("Error: Imagen QR no generada.");
                contentStream.endText();
            }
            contentStream.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}