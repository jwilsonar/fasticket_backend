package pe.edu.pucp.fasticket.services.pago;

import java.awt.*;
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
import pe.edu.pucp.fasticket.exception.BusinessException;
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
        if (dto.getRuc() != null && !dto.getRuc().isBlank()) {
            orden.setRuc(dto.getRuc());
            orden.setRazonSocial(dto.getRazonSocial());
            orden.setDireccionFiscal(dto.getDireccionFiscal());
            ordenRepository.save(orden);
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
        pago.setComprobantePago(comprobante);
        orden.setPago(pago);
        try {
            byte[] pdfBytes = generarComprobantePdf(orden);
            comprobante.setPdfContenido(pdfBytes);
        } catch (IOException e) {
            log.error("Error generando PDF de comprobante para Orden {}", orden.getIdOrdenCompra(), e);
        }
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
                comprobante.getFechaEmision(),
                orden.getRuc(),
                orden.getRazonSocial(),
                orden.getDireccionFiscal()
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
        log.info("Generando PDF (Boleta/Factura) para orden ID: {}", orden.getIdOrdenCompra());

        if (orden.getPago() == null || orden.getPago().getComprobantePago() == null) {
            throw new RuntimeException("La orden no tiene un comprobante de pago asociado");
        }

        ComprobantePago comprobante = orden.getPago().getComprobantePago();
        Pago pago = orden.getPago();
        OrdenResumenDTO ordenDTO = new OrdenResumenDTO(orden, tipoTicketRepositorio);

        // Detectar si es Factura
        boolean esFactura = orden.getRuc() != null && !orden.getRuc().isBlank();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Fuentes
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            float y = 750;
            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("FASTICKET S.A.C.");
            contentStream.endText(); // <--- CERRADO

            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(margin, y - 15);
            contentStream.showText("RUC: 20123456789");
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Av. Universitaria 1801, San Miguel");
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Lima, Perú");
            contentStream.endText();
            float boxWidth = 200;
            float boxHeight = 60;
            float boxX = 550 - boxWidth;
            float boxY = y - 20;
            contentStream.setStrokingColor(Color.BLACK);
            contentStream.addRect(boxX, boxY, boxWidth, boxHeight);
            contentStream.stroke();
            String tituloDoc = esFactura ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA";
            contentStream.beginText();
            contentStream.setFont(fontBold, 12);
            float textWidth = fontBold.getStringWidth(tituloDoc) / 1000 * 12;
            float textX = boxX + (boxWidth - textWidth) / 2;
            contentStream.newLineAtOffset(textX, boxY + 35);
            contentStream.showText(tituloDoc);
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            float serieWidth = fontRegular.getStringWidth("N° " + comprobante.getNumeroSerie()) / 1000 * 12;
            float serieX = boxX + (boxWidth - serieWidth) / 2;
            contentStream.newLineAtOffset(serieX, boxY + 15);
            contentStream.showText("N° " + comprobante.getNumeroSerie());
            contentStream.endText();
            y -= 80;
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText(esFactura ? "RAZÓN SOCIAL:" : "CLIENTE:");
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(margin + 90, y);
            String nombreMostrado = esFactura ? orden.getRazonSocial() :
                    (orden.getCliente().getNombres() + " " + orden.getCliente().getApellidos());
            if (nombreMostrado == null) nombreMostrado = "---";
            contentStream.showText(nombreMostrado.toUpperCase());
            contentStream.endText();
            y -= 15;
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText(esFactura ? "RUC:" : "DOC:");
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(margin + 90, y);
            String docMostrado = esFactura ? orden.getRuc() :
                    (comprobante.getDni() != null ? comprobante.getDni() : orden.getCliente().getDocIdentidad());
            if (docMostrado == null) docMostrado = "---";
            contentStream.showText(docMostrado);
            contentStream.endText();
            if (esFactura && orden.getDireccionFiscal() != null) {
                y -= 15;
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("DIRECCIÓN:");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(margin + 90, y);
                contentStream.showText(orden.getDireccionFiscal());
                contentStream.endText();
            }
            y -= 15;
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("FECHA:");
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(margin + 90, y);
            contentStream.showText(comprobante.getFechaEmision().toLocalDate().toString() + " " +
                    comprobante.getFechaEmision().toLocalTime().toString().substring(0,5));
            contentStream.endText();
            float col2X = 350;
            float eventY = y + (esFactura ? 45 : 30);
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(col2X, eventY);
            contentStream.showText("EVENTO:");
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(col2X + 50, eventY);
            contentStream.showText(ordenDTO.getNombreEvento());
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontRegular, 10);
            contentStream.newLineAtOffset(col2X + 50, eventY - 12);
            contentStream.showText(ordenDTO.getNombreLocal());
            contentStream.endText();
            y -= 30;

            contentStream.moveTo(margin, y);
            contentStream.lineTo(margin + width, y);
            contentStream.stroke();

            y -= 15;

            float colCant = margin;
            float colDesc = margin + 40;
            float colUnit = margin + 350;
            float colTotal = margin + 450;

            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(colCant, y);
            contentStream.showText("CANT");
            contentStream.newLineAtOffset(colDesc - colCant, 0);
            contentStream.showText("DESCRIPCIÓN");
            contentStream.newLineAtOffset(colUnit - colDesc, 0);
            contentStream.showText("P. UNIT");
            contentStream.newLineAtOffset(colTotal - colUnit, 0);
            contentStream.showText("IMPORTE");
            contentStream.endText();

            y -= 5;
            contentStream.moveTo(margin, y);
            contentStream.lineTo(margin + width, y);
            contentStream.stroke();

            y -= 15;

            contentStream.setFont(fontRegular, 10);

            for (ItemResumenDTO item : ordenDTO.getItems()) {
                contentStream.beginText();
                contentStream.newLineAtOffset(colCant, y);
                contentStream.showText(String.valueOf(item.getCantidad()));
                contentStream.endText();
                contentStream.beginText();
                contentStream.newLineAtOffset(colDesc, y);
                contentStream.showText("Entrada " + item.getNombreTipoTicket());
                contentStream.endText();
                contentStream.beginText();
                contentStream.newLineAtOffset(colUnit, y);
                contentStream.showText(String.format("%.2f", item.getPrecioUnitario()));
                contentStream.endText();
                contentStream.beginText();
                contentStream.newLineAtOffset(colTotal, y);
                double importe = item.getPrecioUnitario() * item.getCantidad();
                contentStream.showText(String.format("%.2f", importe));
                contentStream.endText();
                y -= 15;
                if (y < 100) {
                    contentStream.close();
                    PDPage newPage = new PDPage();
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    y = 750;
                    contentStream.setFont(fontRegular, 10);
                }
            }

            y -= 10;
            contentStream.moveTo(margin, y);
            contentStream.lineTo(margin + width, y);
            contentStream.stroke();
            y -= 20;
            float xLabels = 350;
            float xValues = 450;
            if (orden.getSubtotal() != null) {
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(xLabels, y);
                contentStream.showText("SUBTOTAL:");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(xValues, y);
                contentStream.showText("S/ " + String.format("%.2f", orden.getSubtotal()));
                contentStream.endText();
                y -= 15;
            }
            if (orden.getDescuentoPorMembrecia() != null && orden.getDescuentoPorMembrecia() > 0) {
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(xLabels, y);
                contentStream.showText("DSCTO SOCIO:");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(xValues, y);
                contentStream.showText("- S/ " + String.format("%.2f", orden.getDescuentoPorMembrecia()));
                contentStream.endText();
                y -= 15;
            }
            if (orden.getDescuentoPromocional() != null && orden.getDescuentoPromocional() > 0) {
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(xLabels, y);
                contentStream.showText("CUPÓN (" + (orden.getCodigoPromocionalAplicado() != null ? orden.getCodigoPromocionalAplicado() : "") + "):");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(xValues, y);
                contentStream.showText("- S/ " + String.format("%.2f", orden.getDescuentoPromocional()));
                contentStream.endText();
                y -= 15;
            }
            if (orden.getIgv() != null) {
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(xLabels, y);
                contentStream.showText("IGV (18%):");
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.newLineAtOffset(xValues, y);
                contentStream.showText("S/ " + String.format("%.2f", orden.getIgv()));
                contentStream.endText();
                y -= 15;
            }
            y -= 5;
            contentStream.setStrokingColor(Color.BLACK);
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(xLabels - 10, y);
            contentStream.lineTo(margin + width, y);
            contentStream.stroke();
            y -= 20;
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(xLabels, y);
            contentStream.showText("TOTAL:");
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(xValues, y);
            contentStream.showText("S/ " + String.format("%.2f", orden.getTotal()));
            contentStream.endText();
            y -= 50;
            contentStream.beginText();
            contentStream.setFont(fontItalic, 8);
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("Gracias por tu compra. Este documento es un comprobante electrónico generado automáticamente.");
            contentStream.newLineAtOffset(0, -10);
            contentStream.showText("Método de pago: " + (pago.getMetodo() != null ? pago.getMetodo() : "Tarjeta"));
            contentStream.endText();
            contentStream.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            log.info("PDF generado exitosamente (FACTURA/BOLETA) para orden ID: {}", orden.getIdOrdenCompra());
            return baos.toByteArray();
        }
    }
}
