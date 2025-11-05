package pe.edu.pucp.fasticket.services.compra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.compra.*;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.compra.ItemCarritoRepository;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;

import static pe.edu.pucp.fasticket.services.CarroComprasServiceImpl.TIEMPO_RESERVA_MINUTOS;

@Service
@Slf4j
public class OrdenServicio {

    private final OrdenCompraRepositorio ordenCompraRepositorio;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final ClienteRepository clienteRepository;
    private final TicketRepository ticketRepository;
    private final ItemCarritoRepository itemCarritoRepositorio;
    private final CarroComprasRepository carroComprasRepository;
    private final FidelizacionService fidelizacionService;

    public OrdenServicio(
            OrdenCompraRepositorio ordenCompraRepositorio,
            TipoTicketRepositorio tipoTicketRepositorio,
            ClienteRepository clienteRepository,
            TicketRepository ticketRepository,
            ApplicationEventPublisher eventPublisher,
            ItemCarritoRepository itemCarritoRepositorio,
            CarroComprasRepository carroComprasRepository,
            FidelizacionService fidelizacionService
    ) {
        this.ordenCompraRepositorio = ordenCompraRepositorio;
        this.tipoTicketRepositorio = tipoTicketRepositorio;
        this.clienteRepository = clienteRepository;
        this.ticketRepository = ticketRepository;
        this.itemCarritoRepositorio = itemCarritoRepositorio;
        this.carroComprasRepository = carroComprasRepository;
        this.fidelizacionService = fidelizacionService;
    }

    @Transactional
    public OrdenCompra crearOrden(CrearOrdenDTO datosOrden) {
        Cliente cliente = clienteRepository.findById(datosOrden.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + datosOrden.getIdCliente()));

        validarLimitesPorPersona(datosOrden.getItems(), cliente);
        validarStockDisponible(datosOrden.getItems());

        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(cliente);
        orden.setFechaOrden(LocalDate.now());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        OrdenCompra ordenGuardada = ordenCompraRepositorio.save(orden);
        log.info("Orden PENDIENTE ID: {} creada.", ordenGuardada.getIdOrdenCompra());

        List<ItemCarrito> items = construirYGuardarItems(datosOrden.getItems(), cliente, ordenGuardada);
        ordenGuardada.setItems(items);
        ordenGuardada.calcularTotal();
        calcularDescuentoPorMembresia(ordenGuardada, cliente);
        OrdenCompra ordenFinal = ordenCompraRepositorio.save(ordenGuardada);
        cliente.getOrdenesCompra().add(ordenFinal);
        clienteRepository.save(cliente);
        return ordenFinal;
    }

    private void validarLimitesPorPersona(List<ItemSeleccionadoDTO> itemsDTO, Cliente cliente) {
        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + itemDTO.getIdTipoTicket()));

            validarLimitePorPersona(tipoTicket, itemDTO.getCantidad(), cliente);
        }
    }

    private void validarStockDisponible(List<ItemSeleccionadoDTO> itemsDTO) {
        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + itemDTO.getIdTipoTicket()));

            List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, itemDTO.getCantidad())
            );

            if (ticketsDisponibles.size() < itemDTO.getCantidad()) {
                throw new BusinessException("No hay suficientes tickets disponibles para " + tipoTicket.getNombre() +
                        ". Solicitados: " + itemDTO.getCantidad() + ", Disponibles: " + ticketsDisponibles.size());
            }
        }
    }

    private void calcularDescuentoPorMembresia(OrdenCompra orden, Cliente cliente) {
        // Contar total de entradas compradas en la orden
        int totalEntradas = orden.getItems().stream()
                .mapToInt(ItemCarrito::getCantidad)
                .sum();

        // Obtener el tipo de membresía del cliente
        TipoMembresia tipoMembresia = cliente.getNivel();

        // Calcular el porcentaje de descuento según las reglas de negocio
        double porcentajeDescuento = fidelizacionService.calcularDescuentoPorMembresia(tipoMembresia, totalEntradas);

        // Aplicar descuento al subtotal
        Double descuento = orden.getSubtotal() * porcentajeDescuento;
        orden.setDescuentoPorMembrecia(descuento);

        // Recalcular el total después de aplicar el descuento
        orden.aplicarDescuentoYRecalcular();

        log.info("Descuento por membresía aplicado: {} ({}) para cliente ID: {}", descuento, porcentajeDescuento * 100 + "%", cliente.getIdPersona());
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


    private List<ItemCarrito> construirYGuardarItems(List<ItemSeleccionadoDTO> itemsDTO,
                                                     Cliente cliente, OrdenCompra orden) {
        List<ItemCarrito> itemsGuardados = new ArrayList<>();

        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            validarItemYAsistentes(itemDTO);
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + itemDTO.getIdTipoTicket()));
            validarLimitePorPersona(tipoTicket, itemDTO.getCantidad(), cliente);

            ItemCarrito item = new ItemCarrito();
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecio(tipoTicket.getPrecio());
            item.setFechaAgregado(LocalDate.now());
            item.setTipoTicket(tipoTicket);
            item.setOrdenCompra(orden);
            item.calcularPrecioFinal();
            ItemCarrito itemGuardado = itemCarritoRepositorio.save(item);

            List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, itemDTO.getCantidad())
            );
            if (ticketsDisponibles.size() < itemDTO.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para " + tipoTicket.getNombre());
            }

            List<Ticket> ticketsReservados = new ArrayList<>();
            for (int i = 0; i < ticketsDisponibles.size(); i++) {
                Ticket ticket = ticketsDisponibles.get(i);
                DatosAsistenteDTO asistente = itemDTO.getAsistentes().get(i);
                ticket.setEstado(EstadoTicket.RESERVADA);
                ticket.setCliente(cliente);

                Evento evento = tipoTicketRepositorio.findEventoByTipoTicket(tipoTicket.getIdTipoTicket())
                        .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado para el tipo de ticket"));
                ticket.setEvento(evento);

                ticket.setItemCarrito(itemGuardado);
                ticket.setOrdenCompra(orden);
                ticket.setNombreAsistente(asistente.getNombres());
                ticket.setApellidoAsistente(asistente.getApellidos());
                ticket.setTipoDocumentoAsistente(asistente.getTipoDocumento());
                ticket.setDocumentoAsistente(asistente.getNumeroDocumento());
                String codigoQr = generarCodigoQrUnico();
                ticket.setCodigoQr(codigoQr);
                ticket.setQrImage(generarQrComoBytes(codigoQr));

                ticketsReservados.add(ticket);
            }
            ticketRepository.saveAll(ticketsReservados);
            itemGuardado.setTickets(ticketsReservados);
            itemsGuardados.add(itemGuardado);
            int cantidadReservada = itemDTO.getCantidad();
            tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() - cantidadReservada);
            tipoTicket.setCantidadVendida(tipoTicket.getCantidadVendida() + cantidadReservada);
        }
        return itemsGuardados;
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
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (orden.getEstado() != EstadoCompra.PENDIENTE) {
            throw new BusinessException("Solo se pueden confirmar órdenes en estado PENDIENTE");
        }

        CarroCompras carrito = orden.getCarroCompras();
        if (carrito == null) {
            throw new BusinessException("La orden no tiene carrito asociado.");
        }
        List<OrdenCompra> otrasOrdenesActivas = ordenCompraRepositorio
                .findByCarroComprasIdCarroAndActivoTrue(carrito.getIdCarro());
        for (OrdenCompra o : otrasOrdenesActivas) {
            if (!o.getIdOrdenCompra().equals(idOrden)) {
                o.setActivo(false);
                o.setEstado(EstadoCompra.ANULADO);
                ordenCompraRepositorio.save(o);
                log.warn("Orden ID {} del carrito {} marcada como CANCELADA por conflicto de confirmación.",
                        o.getIdOrdenCompra(), carrito.getIdCarro());
            }
        }
        orden.setEstado(EstadoCompra.APROBADO);
        orden.setActivo(false);
        orden.setFechaActualizacion(LocalDate.now());
        ordenCompraRepositorio.save(orden);
        log.info("Orden ID {} confirmada exitosamente.", idOrden);
        Map<Evento, Integer> cantidadPorEvento = new HashMap<>();
        for (ItemCarrito item : orden.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                ticket.setEstado(EstadoTicket.VENDIDA);
            }
            Evento evento = tipoTicketRepositorio.findEventoByTipoTicket(item.getTipoTicket().getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Evento no encontrado para el tipo de ticket " + item.getTipoTicket().getNombre()));
            cantidadPorEvento.merge(evento, item.getCantidad(), Integer::sum);
        }
        for (Map.Entry<Evento, Integer> entry : cantidadPorEvento.entrySet()) {
            Evento evento = entry.getKey();
            Integer cantidadVendida = entry.getValue();
            if (evento.getAforoDisponible() != null) {
                evento.setAforoDisponible(Math.max(evento.getAforoDisponible() - cantidadVendida, 0));
            }
        }
        try {
            itemCarritoRepositorio.deleteByCarroCompraId(carrito.getIdCarro());
        } catch (Exception e) {
            log.warn("No se pudieron eliminar items del carrito ID {}: {}", carrito.getIdCarro(), e.getMessage());
        }
        carrito.setActivo(false);
        carrito.setFechaActualizacion(LocalDateTime.now());
        carroComprasRepository.save(carrito);
        log.info("Carrito ID {} marcado como INACTIVO (histórico).", carrito.getIdCarro());
        CarroCompras nuevoCarro = new CarroCompras();
        nuevoCarro.setCliente(carrito.getCliente());
        nuevoCarro.setActivo(true);
        nuevoCarro.setFechaCreacion(LocalDateTime.now());
        nuevoCarro.setFechaActualizacion(LocalDateTime.now());
        nuevoCarro.setSubtotal(0.0);
        nuevoCarro.setTotal(0.0);
        carroComprasRepository.save(nuevoCarro);
        log.info("Nuevo carrito ID {} creado para cliente ID {}.", nuevoCarro.getIdCarro(),
                nuevoCarro.getCliente() != null ? nuevoCarro.getCliente().getIdPersona() : "N/A");

        fidelizacionService.generarPuntosPorCompra(
                orden.getCliente().getIdPersona(),
                orden.getTotal(),
                orden.getIdOrdenCompra()
        );
        log.info("Puntos generados para cliente ID {} (orden {}).",
                orden.getCliente().getIdPersona(), idOrden);
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
    
    private void validarLimitePorPersona(TipoTicket tipoTicket, Integer cantidad, Cliente cliente) {
        if (tipoTicket.getLimitePorPersona() != null && tipoTicket.getLimitePorPersona() > 0) {
            // Verificar cuántos tickets de este tipo ha comprado el cliente
            Integer ticketsComprados = ticketRepository.countTicketsByClienteAndTipoTicket(cliente.getIdPersona(), tipoTicket.getIdTipoTicket());
            if (ticketsComprados + cantidad > tipoTicket.getLimitePorPersona()) {
                throw new BusinessException("El límite de tickets por persona para '" + tipoTicket.getNombre() + "' es de " + 
                    tipoTicket.getLimitePorPersona() + ". Ya has comprado " + ticketsComprados + " tickets de este tipo.");
            }
        }
    }


    private List<DatosAsistenteDTO> obtenerAsistentesParaItem(ItemCarrito itemCarrito) {
        if (itemCarrito.getTickets() == null || itemCarrito.getTickets().isEmpty()) {
            log.warn("El ItemCarrito ID {} del carrito no tiene tickets asociados.", itemCarrito.getIdItemCarrito());
            return new ArrayList<>(); 
        }

        return itemCarrito.getTickets().stream()
                .map(ticket -> {
                    DatosAsistenteDTO dto = new DatosAsistenteDTO();
                    dto.setTipoDocumento(ticket.getTipoDocumentoAsistente());
                    dto.setNumeroDocumento(ticket.getDocumentoAsistente());
                    dto.setNombres(ticket.getNombreAsistente());
                    dto.setApellidos(ticket.getApellidoAsistente());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdenCompra checkoutDesdeCarrito(Integer idCarrito, List<AsistenteParaItemDTO> itemsConAsistentes) {
        CarroCompras carrito = carroComprasRepository.findById(idCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (!carrito.getActivo() || carrito.getItems().isEmpty()) {
            throw new BusinessException("El carrito está inactivo o vacío.");
        }
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(carrito.getCliente());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(TIEMPO_RESERVA_MINUTOS));
        orden.setCarroCompras(carrito);
        asignarAsistentesATicketsDelCarrito(carrito, itemsConAsistentes, orden);
        for (ItemCarrito item : new ArrayList<>(carrito.getItems())) {
            carrito.removeItem(item);
            orden.addItem(item);
        }
        orden.calcularTotal();
        calcularDescuentoPorMembresia(orden, carrito.getCliente());
        carrito.setActivo(false);
        carrito.setFechaActualizacion(LocalDateTime.now());
        carroComprasRepository.save(carrito);
        OrdenCompra ordenGuardada = ordenCompraRepositorio.save(orden);
        log.info("Orden ID {} creada desde Carrito ID {}.", ordenGuardada.getIdOrdenCompra(), carrito.getIdCarro());

        return ordenGuardada;
    }

    private void asignarAsistentesATicketsDelCarrito(CarroCompras carrito, List<AsistenteParaItemDTO> itemsConAsistentes, OrdenCompra orden) {
        Map<Integer, List<DatosAsistenteDTO>> mapaAsistentes = itemsConAsistentes.stream()
                .collect(Collectors.toMap(AsistenteParaItemDTO::getIdItemCarrito, AsistenteParaItemDTO::getAsistentes));

        for (ItemCarrito item : carrito.getItems()) {
            List<DatosAsistenteDTO> asistentesParaItem = mapaAsistentes.get(item.getIdItemCarrito());
            if (asistentesParaItem == null || asistentesParaItem.size() != item.getCantidad()) {
                throw new IllegalArgumentException("Asistentes no coinciden con el item ID: " + item.getIdItemCarrito());
            }

            for (int i = 0; i < item.getTickets().size(); i++) {
                Ticket ticket = item.getTickets().get(i);
                DatosAsistenteDTO asistente = asistentesParaItem.get(i);

                ticket.setNombreAsistente(asistente.getNombres());
                ticket.setApellidoAsistente(asistente.getApellidos());
                ticket.setTipoDocumentoAsistente(asistente.getTipoDocumento());
                ticket.setDocumentoAsistente(asistente.getNumeroDocumento());

                String codigoQr = generarCodigoQrUnico();
                ticket.setCodigoQr(codigoQr);
                ticket.setQrImage(generarQrComoBytes(codigoQr));
                ticket.setOrdenCompra(orden);
            }
            ticketRepository.saveAll(item.getTickets());
        }
    }
}
