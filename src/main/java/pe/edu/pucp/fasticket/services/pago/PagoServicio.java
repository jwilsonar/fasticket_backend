package pe.edu.pucp.fasticket.services.pago;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.pago.ComprobanteDTO;
import pe.edu.pucp.fasticket.dto.pago.RegistrarPagoDTO;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.notificaciones.PlantillaNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla;
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
import pe.edu.pucp.fasticket.services.S3Service;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;
import pe.edu.pucp.fasticket.services.notificaciones.BrevoEmailService;
import pe.edu.pucp.fasticket.services.notificaciones.PlantillaService;

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
    @Autowired
    private BrevoEmailService brevoEmailService;
    @Autowired
    private PlantillaService plantillaService;
    @Autowired
    private S3Service s3Service;

    @Transactional
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
        
        // Crear y establecer el comprobante
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
        
        // Guardar el comprobante primero para tener el ID
        comprobantePagoRepositorio.save(comprobante);
        comprobantePagoRepositorio.flush();
        
        // Generar el PDF del comprobante y subirlo a S3
        String pdfUrl = null;
        try {
            byte[] pdfBytes = generarComprobantePdf(orden);
            
            // Validar que el PDF sea válido
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("El PDF generado está vacío");
            }
            if (pdfBytes.length < 4 || !new String(pdfBytes, 0, Math.min(4, pdfBytes.length)).equals("%PDF")) {
                throw new RuntimeException("El PDF generado no es un archivo PDF válido");
            }
            
            // Subir el PDF a S3
            String nombreArchivo = "Comprobante_" + comprobante.getNumeroSerie() + ".pdf";
            pdfUrl = s3Service.uploadFileFromBytes(
                pdfBytes, 
                nombreArchivo, 
                "application/pdf", 
                "comprobantes", 
                comprobante.getIdComprobante()
            );
            
            // Guardar la URL del PDF en el comprobante
            comprobante.setPdfUrl(pdfUrl);
            comprobantePagoRepositorio.save(comprobante);
            comprobantePagoRepositorio.flush();
            
            log.info("PDF del comprobante generado y subido a S3 correctamente: {} ({} bytes)", pdfUrl, pdfBytes.length);
        } catch (IOException e) {
            log.error("Error generando PDF de comprobante para Orden {}", orden.getIdOrdenCompra(), e);
            throw new RuntimeException("Error al generar el PDF del comprobante: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error subiendo PDF a S3 para Orden {}", orden.getIdOrdenCompra(), e);
            throw new RuntimeException("Error al subir el PDF a S3: " + e.getMessage(), e);
        }
        
        // Guardar el pago con el comprobante actualizado
        pagoRepository.save(pago);
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
        
        // Enviar correo de confirmación de compra con link al PDF en S3
        enviarCorreoConfirmacionCompra(orden, usuario, comprobante, pdfUrl);
        
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
            
            // Guardar el documento en un ByteArrayOutputStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            document.close();
            
            // Obtener los bytes y validar que sea un PDF válido
            byte[] pdfBytes = baos.toByteArray();
            baos.close();
            
            // Validar que el PDF sea válido (debe empezar con %PDF)
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new IOException("El PDF generado está vacío");
            }
            if (pdfBytes.length < 4 || !new String(pdfBytes, 0, Math.min(4, pdfBytes.length)).equals("%PDF")) {
                throw new IOException("El PDF generado no es un archivo PDF válido");
            }
            
            log.info("PDF generado exitosamente (FACTURA/BOLETA) para orden ID: {} - Tamaño: {} bytes", 
                    orden.getIdOrdenCompra(), pdfBytes.length);
            return pdfBytes;
        }
    }

    /**
     * Envía el correo de confirmación de compra usando BrevoEmailService con la plantilla CONFIRMACION_COMPRA
     * incluyendo un botón para descargar el PDF del comprobante desde S3.
     * 
     * @param orden Orden de compra
     * @param usuario Usuario que realizó la compra
     * @param comprobante Comprobante de pago generado
     * @param pdfUrl URL del PDF del comprobante en S3
     */
    private void enviarCorreoConfirmacionCompra(OrdenCompra orden, pe.edu.pucp.fasticket.model.usuario.Persona usuario, 
                                                 ComprobantePago comprobante, String pdfUrl) {
        try {
            log.info("Iniciando envío de correo de confirmación de compra para orden ID: {}", orden.getIdOrdenCompra());
            
            // Obtener la plantilla de confirmación de compra
            PlantillaNotificacion plantilla = plantillaService.obtenerActiva(TipoPlantilla.CONFIRMACION_COMPRA);
            
            if (plantilla == null || !plantilla.isHabilitado()) {
                log.warn("⚠️ Plantilla CONFIRMACION_COMPRA no encontrada o no habilitada. No se enviará el correo.");
                return;
            }
            
            // Preparar parámetros para la plantilla (incluyendo la URL del PDF)
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("nombre", usuario.getNombres());
            parametros.put("idOrden", orden.getIdOrdenCompra());
            parametros.put("total", String.format("%.2f", orden.getTotal()));
            parametros.put("pdfUrl", pdfUrl != null ? pdfUrl : "");
            
            // Renderizar el contenido HTML con los parámetros
            String asuntoRenderizado = plantillaService.render(plantilla.getAsunto(), parametros);
            String contenidoHtml = plantillaService.render(plantilla.getHtml(), parametros);
            
            // Enviar el correo sin adjuntos (el PDF se descarga desde el botón en el HTML)
            String destinatario = usuario.getEmail();
            String nombreDestinatario = usuario.getNombres() + " " + usuario.getApellidos();
            
            log.info("📧 Enviando correo a: {} con link al PDF en S3 ({}) para orden ID: {}", 
                     destinatario, pdfUrl, orden.getIdOrdenCompra());
            
            boolean enviado = brevoEmailService.enviarEmailHtml(
                destinatario,
                nombreDestinatario,
                asuntoRenderizado,
                contenidoHtml
            );
            
            if (enviado) {
                log.info("✅ Correo de confirmación de compra enviado exitosamente a: {} para orden ID: {} con link al PDF: {}", 
                         destinatario, orden.getIdOrdenCompra(), pdfUrl);
            } else {
                log.error("❌ No se pudo enviar el correo de confirmación de compra a: {} para orden ID: {}", 
                          destinatario, orden.getIdOrdenCompra());
            }
            
        } catch (Exception e) {
            log.error("❌ Error al enviar correo de confirmación de compra para orden ID: {}. Error: {}", 
                      orden.getIdOrdenCompra(), e.getMessage(), e);
            // No lanzamos la excepción para no afectar el flujo principal de la compra
        }
    }
}
