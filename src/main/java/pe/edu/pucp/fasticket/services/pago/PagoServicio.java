package pe.edu.pucp.fasticket.services.pago;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.pago.ComprobanteDTO;
import pe.edu.pucp.fasticket.dto.pago.RegistrarPagoDTO;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.pago.Boleta;
import pe.edu.pucp.fasticket.model.pago.ComprobantePago;
import pe.edu.pucp.fasticket.model.pago.EstadoPago;
import pe.edu.pucp.fasticket.model.pago.Pago;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.pago.BoletaRepositorio;
import pe.edu.pucp.fasticket.repository.pago.ComprobanteDePagoRepositorio;
import pe.edu.pucp.fasticket.repository.pago.PagoRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

@Service
@Slf4j
public class PagoServicio {

    @Autowired
    private PagoRepositorio pagoRepository;
    @Autowired
    private OrdenCompraRepositorio ordenRepository;
    @Autowired
    private OrdenServicio ordenServicio;
    @Autowired
    private ComprobanteDePagoRepositorio comprobantePagoRepositorio;
    @Autowired
    private PersonasRepositorio personaRepositorio;
    @Autowired
    private BoletaRepositorio boletaRepositorio;
    @Autowired
    private TipoTicketRepositorio tipoTicketRepositorio;

    public ComprobanteDTO registrarPagoFinal(RegistrarPagoDTO dto) {
        var orden = ordenRepository.findByIdWithPagoActivo(dto.getIdOrden())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada o con pago inactivo"));
        if (dto.getNumeroTarjeta() == null || dto.getNumeroTarjeta().length() < 4) {
            throw new RuntimeException("Número de tarjeta inválido");
        }
        var usuario = personaRepositorio.findById(orden.getCliente().getIdPersona())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String ultimos4 = dto.getNumeroTarjeta().substring(dto.getNumeroTarjeta().length() - 4);
        Pago pago = new Pago();
        pago.setMetodo("Tarjeta (" + ultimos4 + ")");
        pago.setMonto(dto.getMonto());
        pago.setEstado(EstadoPago.APROBADO);
        pago.setFechaPago(LocalDate.now());
        pago.setActivo(true);
        pago.setFechaCreacion(LocalDate.now());
        pago.setUsuarioCreacion(dto.getIdUsuario());
        pago.setOrdenCompra(orden);
        pagoRepository.save(pago);
        ordenServicio.confirmarPagoOrden(orden.getIdOrdenCompra());
        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setNumeroSerie(String.format("CP-%05d", pago.getIdPago()));
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setTotal(dto.getMonto());
        comprobante.setActivo(true);
        comprobante.setUsuarioCreacion(orden.getCliente().getIdPersona());
        comprobante.setFechaCreacion(LocalDate.now());
        comprobante.setDni(usuario.getDocIdentidad());
        comprobante.setPago(pago);
        comprobantePagoRepositorio.save(comprobante);
        Boleta boleta = new Boleta();
        boleta.setDni(usuario.getDocIdentidad());
        boleta.setNombreCliente(usuario.getNombres() + " " + usuario.getApellidos());
        boleta.setComprobantePago(comprobante);
        boletaRepositorio.save(boleta);
        OrdenResumenDTO ordenDTO = new OrdenResumenDTO(orden, tipoTicketRepositorio);
        List<DatosAsistenteDTO> asistentes = orden.getItems().stream().flatMap(item -> item.getTickets().stream()).map(e -> new DatosAsistenteDTO(
                        e.getTipoDocumentoAsistente(),
                        e.getDocumentoAsistente(),
                        e.getNombreAsistente(),
                        e.getApellidoAsistente()
                ))
                .collect(Collectors.toList());
        return new ComprobanteDTO(
                comprobante.getNumeroSerie(),
                "ORD-" + orden.getIdOrdenCompra(),
                ordenDTO.getNombreEvento(),
                ordenDTO.getNombreLocal(),
                ordenDTO.getFecha(),
                ordenDTO.getHora(),
                orden.getFechaOrden(),
                orden.getFechaOrden().atStartOfDay().toLocalTime(),
                ordenDTO.getItems().stream().mapToInt(ItemResumenDTO::getCantidad).sum(),
                ordenDTO.getItems(),
                asistentes,
                ordenDTO.getTotal(),
                pago.getMetodo(),
                "XXXX-XXXX-XXXX-" + ultimos4,
                pago.getEstado().toString(),
                comprobante.getFechaEmision()
        );
    }

    /**
     * Genera un PDF del comprobante de pago para una orden de compra.
     * 
     * @param orden Orden de compra
     * @return byte[] que representa el archivo PDF del comprobante
     * @throws IOException Si ocurre un error al generar el PDF
     */
    public byte[] generarComprobantePdf(OrdenCompra orden) throws IOException {
        log.info("Generando PDF del comprobante para orden ID: {}", orden.getIdOrdenCompra());
        
        if (orden.getPago() == null || orden.getPago().getComprobantePago() == null) {
            throw new RuntimeException("La orden no tiene un comprobante de pago asociado");
        }
        
        ComprobantePago comprobante = orden.getPago().getComprobantePago();
        Pago pago = orden.getPago();
        OrdenResumenDTO ordenDTO = new OrdenResumenDTO(orden, tipoTicketRepositorio);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            float yPosition = 750;
            float margin = 50;
            
            // Título
            contentStream.beginText();
            contentStream.setFont(fontBold, 20);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("COMPROBANTE DE PAGO");
            contentStream.endText();
            yPosition -= 40;
            
            // Información del comprobante
            contentStream.beginText();
            contentStream.setFont(fontBold, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Número de Serie: " + comprobante.getNumeroSerie());
            contentStream.newLineAtOffset(0, -20);
            contentStream.setFont(fontRegular, 12);
            contentStream.showText("Código de Compra: ORD-" + orden.getIdOrdenCompra());
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Fecha de Emisión: " + 
                (comprobante.getFechaEmision() != null ? comprobante.getFechaEmision().toString() : "N/A"));
            contentStream.endText();
            yPosition -= 60;
            
            // Información del evento
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Evento: " + ordenDTO.getNombreEvento());
            contentStream.newLineAtOffset(0, -20);
            contentStream.setFont(fontRegular, 12);
            contentStream.showText("Local: " + ordenDTO.getNombreLocal());
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Fecha del Evento: " + 
                (ordenDTO.getFecha() != null ? ordenDTO.getFecha().toString() : "N/A"));
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Hora: " + 
                (ordenDTO.getHora() != null ? ordenDTO.getHora().toString() : "N/A"));
            contentStream.endText();
            yPosition -= 80;
            
            // Items
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Detalle de Compra:");
            contentStream.endText();
            yPosition -= 30;
            
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(margin, yPosition);
            for (ItemResumenDTO item : ordenDTO.getItems()) {
                String itemText = String.format("%s x%d - S/ %.2f", 
                    item.getNombreTipoTicket(), item.getCantidad(), item.getPrecioUnitario());
                contentStream.showText(itemText);
                contentStream.newLineAtOffset(0, -15);
                yPosition -= 15;
                if (yPosition < 100) {
                    contentStream.endText();
                    PDPage newPage = new PDPage();
                    document.addPage(newPage);
                    contentStream.close();
                    contentStream = new PDPageContentStream(document, newPage);
                    yPosition = 750;
                    contentStream.beginText();
                    contentStream.setFont(fontRegular, 10);
                    contentStream.newLineAtOffset(margin, yPosition);
                }
            }
            contentStream.endText();
            yPosition -= 30;
            
            // Total
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Total: S/ " + String.format("%.2f", orden.getTotal()));
            contentStream.endText();
            yPosition -= 30;
            
            // Información de pago
            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Método de Pago: " + (pago.getMetodo() != null ? pago.getMetodo() : "N/A"));
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("Estado: " + (pago.getEstado() != null ? pago.getEstado().toString() : "N/A"));
            contentStream.endText();
            
            contentStream.close();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            log.info("PDF del comprobante generado exitosamente para orden ID: {}", orden.getIdOrdenCompra());
            return baos.toByteArray();
        }
    }
}
