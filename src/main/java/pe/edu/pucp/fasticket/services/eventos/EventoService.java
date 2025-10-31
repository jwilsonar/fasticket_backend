package pe.edu.pucp.fasticket.services.eventos;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoDetalleDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalDetalleDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketCompraDTO;
import pe.edu.pucp.fasticket.dto.reportes.*;
import pe.edu.pucp.fasticket.events.EventoCanceladoEvent;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.EventoMapper;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventoService {

    private final EventosRepositorio eventoRepository;
    private final LocalesRepositorio localRepository;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final OrdenCompraRepositorio ordenCompraRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EventoMapper eventoMapper;

    public List<EventoResponseDTO> listarTodos() {
        return eventoRepository.findAll().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventoResponseDTO> listarActivos() {
        return eventoRepository.findByActivoTrue().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventoResponseDTO> listarProximos() {
        return eventoRepository.findEventosProximos(LocalDate.now()).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventoResponseDTO> listarPorEstado(EstadoEvento estado) {
        return eventoRepository.findByEstadoEventoAndActivoTrue(estado).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EventoResponseDTO obtenerPorId(Integer id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
        return eventoMapper.toResponseDTO(evento);
    }

    @Transactional
    public EventoResponseDTO crear(EventoCreateDTO dto) {
        log.info("Creando nuevo evento: {}", dto.getNombre());

        // Validar fecha futura
        if (dto.getFechaEvento().isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha del evento debe ser futura");
        }

        // Obtener local
        Local local = null;
        if (dto.getIdLocal() != null) {
            local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + dto.getIdLocal()));
        }

        // Crear y guardar
        Evento evento = eventoMapper.toEntity(dto, local);
        Evento eventoGuardado = eventoRepository.save(evento);

        log.info("Evento creado con ID: {}", eventoGuardado.getIdEvento());
        return eventoMapper.toResponseDTO(eventoGuardado);
    }

    @Transactional
    public EventoResponseDTO actualizar(Integer id, EventoCreateDTO dto) {
        log.info("Actualizando evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        // Obtener local
        Local local = null;
        if (dto.getIdLocal() != null) {
            local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + dto.getIdLocal()));
        }

        // Actualizar
        eventoMapper.updateEntity(evento, dto, local);
        Evento eventoActualizado = eventoRepository.save(evento);

        log.info("Evento actualizado: {}", id);
        return eventoMapper.toResponseDTO(eventoActualizado);
    }

    /**
     * Actualiza únicamente la URL de la imagen de un evento.
     * 
     * @param id ID del evento
     * @param imagenUrl URL de la imagen a guardar
     * @return EventoResponseDTO con la información actualizada
     */
    @Transactional
    public EventoResponseDTO actualizarImagenUrl(Integer id, String imagenUrl) {
        log.info("Actualizando URL de imagen para evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        evento.setImagenUrl(imagenUrl);
        evento.setFechaActualizacion(LocalDate.now());
        Evento eventoActualizado = eventoRepository.save(evento);

        log.info("URL de imagen actualizada para evento ID: {}", id);
        return eventoMapper.toResponseDTO(eventoActualizado);
    }

    @Transactional
    public void eliminarLogico(Integer id) {
        log.info("Eliminación lógica del evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        evento.setActivo(false);
        evento.setFechaActualizacion(LocalDate.now());
        eventoRepository.save(evento);

        log.info("Evento desactivado: {}", id);
    }
    /**
     * RF-065: Filtra eventos por tipo/categoría.
     * 
     * @param tipoEvento Tipo de evento (CONCIERTO, TEATRO, DEPORTIVO, etc.)
     * @return Lista de eventos del tipo especificado
     */
    public List<EventoResponseDTO> listarPorTipo(String tipoEvento) {
        log.info("Buscando eventos por tipo: {}", tipoEvento);
        return eventoRepository.findByTipoEventoAndActivoTrue(tipoEvento).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-066: Filtra eventos por rango de fechas.
     * 
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de eventos en el rango de fechas
     */
    public List<EventoResponseDTO> listarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Buscando eventos entre {} y {}", fechaInicio, fechaFin);
        return eventoRepository.findByFechaEventoBetweenAndActivoTrue(fechaInicio, fechaFin).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-067: Filtra eventos por ubicación (distrito del local).
     * 
     * @param idDistrito ID del distrito
     * @return Lista de eventos en el distrito especificado
     */
    public List<EventoResponseDTO> listarPorDistrito(Integer idDistrito) {
        log.info("Buscando eventos en distrito ID: {}", idDistrito);
        return eventoRepository.findByLocalDistritoIdDistritoAndActivoTrue(idDistrito).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-069: Ordena eventos por fecha de inicio.
     * 
     * @return Lista de eventos ordenados por fecha ascendente
     */
    public List<EventoResponseDTO> listarOrdenadosPorFecha() {
        log.info("Listando eventos ordenados por fecha");
        return eventoRepository.findByActivoTrueOrderByFechaEventoAsc().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-016: Cancela un evento cambiando su estado a CANCELADO.
     * TODO: Implementar notificaciones a clientes que compraron tickets.
     * 
     * @param id ID del evento a cancelar
     */
    @Transactional
    public void cancelarEvento(Integer id) {
        log.info("Cancelando evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        if (evento.getEstadoEvento() == EstadoEvento.CANCELADO) {
            throw new BusinessException("El evento ya está cancelado");
        }

        evento.setEstadoEvento(EstadoEvento.CANCELADO);
        evento.setFechaActualizacion(LocalDate.now());
        eventoRepository.save(evento);

        log.info("Evento cancelado: {}. Se deben enviar notificaciones a los compradores.", id);
        
        // RF-016: Publicar evento para notificar a los compradores (Patrón Observer)
        try {
            // Obtener emails de todos los clientes que compraron tickets para este evento
            List<String> emailsAfectados = evento.getTickets().stream()
                .map(ticket -> ticket.getCliente().getEmail())
                .distinct()
                .collect(Collectors.toList());
            
            String motivo = "Cancelación administrativa del evento";
            
            log.info("📢 Publicando evento EventoCanceladoEvent. Afectados: {} clientes", emailsAfectados.size());
            eventPublisher.publishEvent(new EventoCanceladoEvent(evento, motivo, emailsAfectados));
        } catch (Exception e) {
            log.error("⚠️ Error al publicar evento de cancelación (no crítico): {}", e.getMessage());
        }
    }
    @Transactional(readOnly = true)
    public EventoDetalleDTO obtenerDetalleParaCompra(Integer id) {
        log.info("INICIO: Obteniendo detalles para compra del evento ID: {}", id);

        // 1. Buscar el evento
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        if (Boolean.FALSE.equals(evento.getActivo())) {
            throw new ResourceNotFoundException("Evento no disponible para compra con ID: " + id);
        }

        // 2. Mapeo básico del evento
        EventoDetalleDTO dto = new EventoDetalleDTO();
        dto.setId(evento.getIdEvento());
        dto.setNombre(evento.getNombre());
        dto.setFecha(evento.getFechaEvento());
        dto.setHora(evento.getHoraInicio());
        dto.setUrlImagen(evento.getImagenUrl());
        dto.setDescripcion(evento.getDescripcion());

        // 3. Validar y mapear Local
        Local local = evento.getLocal();
        if (local == null || !Boolean.TRUE.equals(local.getActivo())) {
            throw new BusinessException("El local del evento no está disponible para compras");
        }

        LocalDetalleDTO localDTO = new LocalDetalleDTO();
        localDTO.setNombre(local.getNombre());
        localDTO.setDireccion(local.getDireccion());
        localDTO.setUrlMapa(local.getUrlMapa());
        localDTO.setDistrito(local.getDistrito());
        dto.setLocal(localDTO);

        // 4. Mapeo limpio de Tipos de Ticket - obtener a través de las zonas del local
        List<TipoTicketCompraDTO> tiposDTO = evento.getLocal().getZonas().stream()
                .flatMap(zona -> tipoTicketRepositorio.findByZonaIdZonaAndActivoTrue(zona.getIdZona()).stream())
                .filter(tt -> tt.getCantidadDisponible() > 0)
                .map(tt -> {
                    TipoTicketCompraDTO t = new TipoTicketCompraDTO();
                    t.setIdTipoTicket(tt.getIdTipoTicket());
                    t.setNombre(tt.getNombre());
                    t.setDescripcion(tt.getDescripcion());
                    t.setPrecio(tt.getPrecio());
                    t.setCantidadDisponible(tt.getCantidadDisponible());
                    return t;
                })
                .collect(Collectors.toList());

        if (tiposDTO.isEmpty()) {
            throw new BusinessException("El evento no tiene tickets disponibles actualmente");
        }

        dto.setTiposDeTicket(tiposDTO);

        log.info("FIN: Detalle de evento {} mapeado correctamente.", id);
        return dto;
    }

    /**
     * RF-034: Genera un reporte de ventas en formato PDF para un evento específico.
     * @param idEvento ID del evento
     * @return byte[] que representa el archivo PDF.
     * @throws IOException Si ocurre un error al generar el PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generarReporteVentasPdf(Integer idEvento) throws IOException {
        log.info("Generando reporte PDF de ventas para evento ID: {}", idEvento);

        // --- 1. Obtener Datos ---
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        List<OrdenCompra> ordenesAprobadas = ordenCompraRepository.findByItems_TipoTicket_Evento_IdEventoAndEstado(idEvento, EstadoCompra.APROBADO);

        if (ordenesAprobadas.isEmpty()) {
            log.warn("No se encontraron ventas aprobadas para el evento ID: {}", idEvento);
        }

        // --- 2. Calcular Métricas y Poblar DTOs ---
        ReporteVentasEventoDTO datosReporte = calcularMetricasReporte(evento, ordenesAprobadas);

        // --- 3. Generar el PDF ---
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Fuentes estándar
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // Posición inicial
            float yPosition = 750;
            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;

            // --- Escribir Contenido del PDF ---
            contentStream.beginText();
            contentStream.setFont(fontBold, 16);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Reporte de Ventas - Evento");
            contentStream.endText();
            yPosition -= 30; // Bajar línea

            // Detalles del Evento
            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Evento: " + datosReporte.getEventoDetalles().getTitulo() + " (ID: " + idEvento + ")");
            contentStream.newLineAtOffset(0, -15); // Bajar línea
            contentStream.showText("Fecha: " + datosReporte.getEventoDetalles().getFechaEvento().toString());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Local: " + datosReporte.getEventoDetalles().getLocalNombre());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Generado: " + datosReporte.getReporteInfo().getFechaGeneracion().toString());
            contentStream.endText();
            yPosition -= 60;

            // Resumen General
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Resumen General de Ventas");
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Tickets Vendidos: " + datosReporte.getResumenGeneralVentas().getTicketsVendidosTotal());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText(String.format("Ingresos Brutos: S/ %.2f", datosReporte.getResumenGeneralVentas().getIngresosBrutosTotal()));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText(String.format("Descuentos Aplicados: S/ %.2f", datosReporte.getResumenGeneralVentas().getDescuentosAplicadosTotal()));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText(String.format("Ingresos Netos: S/ %.2f", datosReporte.getResumenGeneralVentas().getIngresosNetosTotal()));
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText(String.format("Ocupación: %.1f%%", datosReporte.getResumenGeneralVentas().getPorcentajeOcupacion()));
            contentStream.endText();
            yPosition -= 90;

            // Desglose por Categoría (Ejemplo básico - Habría que iterar y formatear mejor)
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Desglose por Categoría");
            contentStream.endText();
            yPosition -= 20;

            contentStream.setFont(fontRegular, 10);
            for (DesgloseCategoriaTicketDTO categoria : datosReporte.getDesglosePorCategoriaTicket()) {
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(String.format("- %s: Vendidos=%d, Ingresos Netos=S/ %.2f",
                        categoria.getCategoriaNombre(),
                        categoria.getTicketsVendidos(),
                        categoria.getIngresosNetosCategoria()));
                contentStream.endText();
                yPosition -= 12;
                if (yPosition < margin) { // Salto de página simple
                    contentStream.close();
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 750;
                    contentStream.setFont(fontRegular, 10); // Reestablecer fuente
                }
            }

            // --- Fin Contenido ---
            contentStream.close();
            document.save(outputStream);
        }
        log.info("Reporte PDF generado exitosamente para evento ID: {}", idEvento);
        return outputStream.toByteArray();
    }

    // --- MÉTODO AUXILIAR PARA CÁLCULOS (CORREGIDO) ---
    private ReporteVentasEventoDTO calcularMetricasReporte(Evento evento, List<OrdenCompra> ordenesAprobadas) {
        ReporteVentasEventoDTO dto = new ReporteVentasEventoDTO();
        dto.setReporteInfo(new ReporteInfoDTO());
        dto.getReporteInfo().setFechaGeneracion(LocalDateTime.now());
        dto.getReporteInfo().setPeriodoCubierto(evento.getEstadoEvento() == EstadoEvento.FINALIZADO ? "Completo" : "Parcial");

        dto.setEventoDetalles(new EventoDetallesDTO());
        dto.getEventoDetalles().setIdEvento(evento.getIdEvento());
        dto.getEventoDetalles().setTitulo(evento.getNombre());
        dto.getEventoDetalles().setFechaEvento(evento.getFechaEvento());
        if (evento.getLocal() != null) {
            dto.getEventoDetalles().setLocalNombre(evento.getLocal().getNombre());
            dto.getEventoDetalles().setAforoTotal(evento.getLocal().getAforoTotal());
        } else {
            dto.getEventoDetalles().setLocalNombre("N/A");
            dto.getEventoDetalles().setAforoTotal(0);
        }

        dto.setResumenGeneralVentas(new ResumenGeneralVentasDTO());
        Map<String, DesgloseCategoriaTicketDTO> desgloseMap = new HashMap<>(); // Para agrupar por categoría

        long totalTicketsVendidos = 0;
        double totalIngresosBrutos = 0.0;
        double totalDescuentos = 0.0;
        double totalIngresosNetos = 0.0; // Usaremos el total de la orden

        for (OrdenCompra orden : ordenesAprobadas) {
            // El descuento y el neto se calculan a nivel de Orden de Compra
            totalDescuentos += (orden.getDescuentoPorMembrecia() != null ? orden.getDescuentoPorMembrecia() : 0.0) + 
                               (orden.getDescuentoPorCanje() != null ? orden.getDescuentoPorCanje() : 0.0);
            totalIngresosNetos += orden.getTotal(); // Suma el total neto pagado

            for (ItemCarrito item : orden.getItems()) {
                // La lógica de cálculo debe basarse en 'TipoTicket', no en 'Ticket'
                TipoTicket tipoTicket = item.getTipoTicket(); // <-- Corrección 1: Usar getTipoTicket()

                // Asegurarse que este item pertenece al evento del reporte
                if (tipoTicket != null && tipoTicket.getEvento().getIdEvento().equals(evento.getIdEvento())) {

                    long cantidadItem = item.getCantidad(); // <-- OK (ItemCarrito tiene getCantidad())
                    totalTicketsVendidos += cantidadItem;

                    // Ingreso Bruto es Precio Base (de ItemCarrito) * Cantidad
                    double subtotalItem = item.getPrecio() * cantidadItem; // <-- Corrección 2: Usar getPrecio()
                    totalIngresosBrutos += subtotalItem;

                    // Lógica de Desglose
                    String categoriaKey = tipoTicket.getNombre(); // La clave es el nombre del TipoTicket

                    DesgloseCategoriaTicketDTO desglose = desgloseMap.computeIfAbsent(categoriaKey, k -> {
                        DesgloseCategoriaTicketDTO nuevo = new DesgloseCategoriaTicketDTO();
                        nuevo.setCategoriaNombre(k);
                        nuevo.setPrecioUnitarioBase(tipoTicket.getPrecio()); // Precio base del TipoTicket
                        nuevo.setTicketsDisponibles(tipoTicket.getStock()); // <-- Corrección 3: Usar getStock()
                        return nuevo;
                    });

                    desglose.setTicketsVendidos(desglose.getTicketsVendidos() + cantidadItem);
                    desglose.setIngresosBrutosCategoria(desglose.getIngresosBrutosCategoria() + subtotalItem);
                    desglose.setIngresosNetosCategoria(desglose.getIngresosNetosCategoria() + item.getPrecioFinal()); // <-- OK (ItemCarrito tiene getPrecioFinal())
                }
            }
        }

        dto.getResumenGeneralVentas().setTicketsVendidosTotal(totalTicketsVendidos);
        dto.getResumenGeneralVentas().setIngresosBrutosTotal(totalIngresosBrutos);
        dto.getResumenGeneralVentas().setDescuentosAplicadosTotal(totalDescuentos);
        dto.getResumenGeneralVentas().setIngresosNetosTotal(totalIngresosNetos); // El neto total es la suma de los totales de las órdenes

        if (dto.getEventoDetalles().getAforoTotal() != null && dto.getEventoDetalles().getAforoTotal() > 0) {
            dto.getResumenGeneralVentas().setPorcentajeOcupacion(
                    ((double) totalTicketsVendidos / dto.getEventoDetalles().getAforoTotal()) * 100.0
            );
        }

        // Finalizar cálculos de desglose (porcentaje)
        desgloseMap.values().forEach(d -> {
            if (d.getTicketsDisponibles() != null && d.getTicketsDisponibles() > 0) {
                d.setPorcentajeVentasCategoria(((double) d.getTicketsVendidos() / d.getTicketsDisponibles()) * 100.0);
            }
        });
        dto.setDesglosePorCategoriaTicket(new ArrayList<>(desgloseMap.values()));

        // TODO: Calcular tendencia por fecha si se requiere

        return dto;
    }
    /**
     * PASO FINAL del Wizard: Publica un evento.
     * Cambia el estado de BORRADOR a PUBLICADO.
     * RF-014: Valida que el evento tenga al menos una entrada (Ticket/Categoria)
     */
    @Transactional
    public EventoResponseDTO publicarEvento(Integer idEvento) {
        log.info("Intentando publicar evento ID: {}", idEvento);

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        // 1. Validar estado
        if (evento.getEstadoEvento() != EstadoEvento.BORRADOR) {
            throw new BusinessException("Solo se pueden publicar eventos en estado BORRADOR");
        }

        // 2. Validar RF-014: Que tenga al menos una entrada/ticket
        // Esta validación ahora usa la relación 'tiposTicket' que sí existe en tu Evento.java
        if (evento.getTiposTicket() == null || evento.getTiposTicket().isEmpty()) {
            throw new BusinessException("Error: No se ha definido el mínimo de categorías de entradas.");
        }

        // 3. ¡Publicar!
        evento.setEstadoEvento(EstadoEvento.PUBLICADO);
        evento.setActivo(true);
        evento.setFechaActualizacion(LocalDate.now());

        Evento eventoPublicado = eventoRepository.save(evento);

        log.info("¡Evento ID: {} publicado exitosamente!", idEvento);
        return eventoMapper.toResponseDTO(eventoPublicado);
    }
}

