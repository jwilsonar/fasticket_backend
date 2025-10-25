package pe.edu.pucp.fasticket.services.compra;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.compra.*;
import pe.edu.pucp.fasticket.events.CompraAnuladaEvent;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrdenServicio {

    private final OrdenCompraRepositorio ordenCompraRepositorio;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final ClienteRepository clienteRepository;
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrdenServicio(
            OrdenCompraRepositorio ordenCompraRepositorio,
            TipoTicketRepositorio tipoTicketRepositorio,
            ClienteRepository clienteRepository,
            TicketRepository ticketRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.ordenCompraRepositorio = ordenCompraRepositorio;
        this.tipoTicketRepositorio = tipoTicketRepositorio;
        this.clienteRepository = clienteRepository;
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrdenCompra crearOrden(CrearOrdenDTO datosOrden) {
        Cliente cliente = clienteRepository.findById(datosOrden.getIdCliente()).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + datosOrden.getIdCliente()));
        List<ItemCarrito> items = construirItemsDesdeDTO(datosOrden.getItems(), cliente);
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(cliente);
        orden.setFechaOrden(LocalDate.now());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setItems(items);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        for (ItemCarrito item : items) {
            item.setOrdenCompra(orden);
        }
        orden.calcularTotal();
        log.info("Guardando nueva orden ID temporal {} para cliente ID {}", orden.hashCode(), cliente.getIdPersona());
        return ordenCompraRepositorio.save(orden);
    }

    protected OrdenCompra registrarOrdenCompra(Cliente cliente, List<ItemCarrito> itemsCarrito) {
        OrdenCompra ordenCompra = new OrdenCompra();
        ordenCompra.setCliente(cliente);
        ordenCompra.setFechaCreacion(LocalDate.now());
        ordenCompra.setEstado(EstadoCompra.PENDIENTE);
        ordenCompra.setActivo(true);
        if (itemsCarrito != null && !itemsCarrito.isEmpty()) {
            for (ItemCarrito item : itemsCarrito) {
                item.setOrdenCompra(ordenCompra);
                if (item.getTickets() != null) {
                    for (Ticket ticket : item.getTickets()) {
                        ticket.setItemCarrito(item);
                    }
                }
            }
            ordenCompra.setItems(itemsCarrito);
        } else {
            ordenCompra.setItems(new ArrayList<>());
        }
        cliente.getOrdenesCompra().add(ordenCompra);
        return ordenCompraRepositorio.save(ordenCompra);
    }


    private List<ItemCarrito> construirItemsDesdeDTO(List<ItemSeleccionadoDTO> itemsDTO, Cliente cliente) {
        List<ItemCarrito> items = new ArrayList<>();

        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            validarItemYAsistentes(itemDTO);
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket()).orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + itemDTO.getIdTipoTicket()));
            Integer edadCliente = cliente.calcularEdad();
            Integer edadMinima = tipoTicket.getEvento().getEdadMinima();
            if (edadMinima != null && edadMinima > 0 && edadCliente != null && edadCliente < edadMinima) {
                throw new IllegalArgumentException("El evento '%s' requiere edad mínima...");
            }
            List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket,
                    EstadoTicket.DISPONIBLE,
                    PageRequest.of(0, itemDTO.getCantidad())
            );
            if (ticketsDisponibles.size() < itemDTO.getCantidad()) {
                throw new RuntimeException("No hay suficientes tickets disponibles para " + tipoTicket.getNombre());
            }
            ItemCarrito item = new ItemCarrito();
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecio(tipoTicket.getPrecio());
            item.setDescuento(0.0);
            item.setActivo(true);
            item.setFechaAgregado(LocalDate.now());
            item.setTipoTicket(tipoTicket);
            item.calcularPrecioFinal();
            if (ticketsDisponibles.size() != itemDTO.getAsistentes().size()) {
                throw new IllegalStateException("Inconsistencia entre tickets encontrados y asistentes.");
            }
            for (int i = 0; i < ticketsDisponibles.size(); i++) {
                Ticket ticket = ticketsDisponibles.get(i);
                DatosAsistenteDTO asistente = itemDTO.getAsistentes().get(i);
                ticket.setEstado(EstadoTicket.RESERVADA);
                ticket.setItemCarrito(item);
                ticket.setCliente(cliente);
                ticket.setTipoDocumentoAsistente(asistente.getTipoDocumento());
                ticket.setDocumentoAsistente(asistente.getNumeroDocumento());
                ticket.setNombreAsistente(asistente.getNombres());
                ticket.setApellidoAsistente(asistente.getApellidos());
                String codigoQr = generarCodigoQrUnico();
                ticket.setCodigoQr(codigoQr);
                ticket.setQrImage(generarQrComoBytes(codigoQr));
            }
            item.setTickets(ticketsDisponibles);
            items.add(item);
        }
        return items;
    }

    public OrdenResumenDTO generarResumenOrden(CrearOrdenDTO datosOrden) {
        List<ItemResumenDTO> resumenItems = new ArrayList<>();
        double subtotal = 0.0;

        for (ItemSeleccionadoDTO item : datosOrden.getItems()) {
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(item.getIdTipoTicket()).orElseThrow(() -> new RuntimeException("Tipo de ticket no encontrado con id: " + item.getIdTipoTicket()));
            ItemResumenDTO itemResumen = new ItemResumenDTO();
            itemResumen.setNombreTipoTicket(tipoTicket.getNombre());
            itemResumen.setCantidad(item.getCantidad());
            itemResumen.setPrecioUnitario(tipoTicket.getPrecio());
            subtotal += tipoTicket.getPrecio() * item.getCantidad();
            resumenItems.add(itemResumen);
        }
        OrdenResumenDTO resumen = new OrdenResumenDTO();
        resumen.setItems(resumenItems);
        resumen.setSubtotal(subtotal);
        resumen.setTotal(subtotal);

        return resumen;
    }

    @Transactional
    public void confirmarPagoOrden(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setEstado(EstadoCompra.APROBADO);
        for (ItemCarrito item : orden.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                ticket.setEstado(EstadoTicket.VENDIDA);
            }
        }
        ordenCompraRepositorio.save(orden);
    }

    @Transactional
    public void cancelarOrden(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setEstado(EstadoCompra.RECHAZADO);
        for (ItemCarrito item : orden.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                ticket.setEstado(EstadoTicket.DISPONIBLE);
                ticket.setActivo(false);
            }
            TipoTicket tipo = item.getTipoTicket();
            tipo.setCantidadDisponible(tipo.getCantidadDisponible() + item.getCantidad());
            tipoTicketRepositorio.save(tipo);
        }
        ordenCompraRepositorio.save(orden);
    }

    private String generarCodigoQrUnico() {
        return java.util.UUID.randomUUID().toString();
    }

    private byte[] generarQrComoBytes(String contenido) {
        try {
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            var matrix = writer.encode(contenido, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);

            java.awt.image.BufferedImage image =
                    new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando QR", e);
        }
    }

    private void validarItemYAsistentes(ItemSeleccionadoDTO item) {
        if (item.getAsistentes() == null || item.getAsistentes().size() != item.getCantidad()) {
            throw new IllegalArgumentException("La cantidad de asistentes no coincide con la cantidad solicitada");
        }
        for (DatosAsistenteDTO a : item.getAsistentes()) {
            if (a.getNombres() == null || a.getNombres().isBlank())
                throw new IllegalArgumentException("Nombre asistente obligatorio");
            if (a.getNumeroDocumento() == null || a.getNumeroDocumento().isBlank())
                throw new IllegalArgumentException("Documento asistente obligatorio");
            if (a.getTipoDocumento() == null)
                throw new IllegalArgumentException("Tipo de documento obligatorio");
        }
    }

    /**
     * RF-089: Permite al administrador anular una compra.
     * RF-090: Revierte los cupos al stock al anular la compra.
     * 
     * @param idOrden ID de la orden a anular
     */
    @Transactional
    public void anularCompra(Integer idOrden) {
        log.info("Anulando compra ID: {}", idOrden);
        
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

        // Validar que la orden esté en un estado que permita anulación
        if (orden.getEstado() == EstadoCompra.RECHAZADO || orden.getEstado() == EstadoCompra.ANULADO) {
            throw new IllegalArgumentException("La orden ya está rechazada o anulada");
        }

        // Cambiar estado de la orden
        orden.setEstado(EstadoCompra.ANULADO);
        orden.setFechaActualizacion(LocalDate.now());

        // RF-090: Revertir cupos al stock
        for (ItemCarrito item : orden.getItems()) {
            // Invalidar todos los tickets
            for (Ticket ticket : item.getTickets()) {
                ticket.setEstado(EstadoTicket.ANULADA);
                ticket.setActivo(false);
            }
            
            // Devolver stock
            TipoTicket tipoTicket = item.getTipoTicket();
            tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() + item.getCantidad());
            tipoTicket.setCantidadVendida(tipoTicket.getCantidadVendida() - item.getCantidad());
            tipoTicketRepositorio.save(tipoTicket);
            
            log.info("Devueltos {} tickets del tipo '{}' al stock", item.getCantidad(), tipoTicket.getNombre());
        }

        ordenCompraRepositorio.save(orden);
        log.info("Compra anulada exitosamente. Orden ID: {}", idOrden);
        
        // RF-089: Publicar evento para notificar al cliente (Patrón Observer)
        try {
            String emailCliente = orden.getCliente().getEmail();
            String motivo = "Anulación solicitada por el administrador del sistema";
            
            log.info("📢 Publicando evento CompraAnuladaEvent para orden #{}", idOrden);
            eventPublisher.publishEvent(new CompraAnuladaEvent(orden, emailCliente, motivo));
        } catch (Exception e) {
            log.error("⚠️ Error al publicar evento de compra anulada (no crítico): {}", e.getMessage());
        }
    }

}
