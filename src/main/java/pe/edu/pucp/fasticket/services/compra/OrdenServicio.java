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
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemSeleccionadoDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.RegistrarParticipantesDTO;
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

@Service
@Slf4j
public class OrdenServicio {

    private final OrdenCompraRepositorio ordenCompraRepositorio;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final ClienteRepository clienteRepository;
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ItemCarritoRepository itemCarritoRepositorio;
    private final CarroComprasRepository carroComprasRepository;

    public OrdenServicio(
            OrdenCompraRepositorio ordenCompraRepositorio,
            TipoTicketRepositorio tipoTicketRepositorio,
            ClienteRepository clienteRepository,
            TicketRepository ticketRepository,
            ApplicationEventPublisher eventPublisher,
            ItemCarritoRepository itemCarritoRepositorio,
            CarroComprasRepository carroComprasRepository
    ) {
        this.ordenCompraRepositorio = ordenCompraRepositorio;
        this.tipoTicketRepositorio = tipoTicketRepositorio;
        this.clienteRepository = clienteRepository;
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
        this.itemCarritoRepositorio = itemCarritoRepositorio;
        this.carroComprasRepository = carroComprasRepository;
    }

    @Transactional
    public OrdenCompra crearOrden(CrearOrdenDTO datosOrden) {
        Cliente cliente = clienteRepository.findById(datosOrden.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + datosOrden.getIdCliente()));

        List<ItemCarrito> items = construirItemsDesdeDTO(datosOrden.getItems(), cliente);
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(cliente);
        orden.setFechaOrden(LocalDate.now());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        for (ItemCarrito item : items) {
            item.setOrdenCompra(orden);
            for (Ticket ticket : item.getTickets()) {
                ticket.setItemCarrito(item);
                ticket.setOrdenCompra(orden); // Asociar ticket con la orden
                ticket.setEstado(EstadoTicket.RESERVADA);
            }
        }
        orden.setItems(items);
        orden.calcularTotal();
        OrdenCompra ordenGuardada = ordenCompraRepositorio.save(orden);
        
        // Guardar explícitamente todos los tickets
        for (ItemCarrito item : ordenGuardada.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                ticketRepository.save(ticket);
            }
        }
        
        // Actualizar el historial de compras del cliente
        cliente.getOrdenesCompra().add(ordenGuardada);
        clienteRepository.save(cliente);
        
        return ordenGuardada;
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
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + itemDTO.getIdTipoTicket()));
            Integer edadCliente = cliente.calcularEdad();
            // Necesitamos encontrar el evento asociado a este tipo de ticket
            // Como TipoTicket no tiene relación directa con Evento, necesitamos buscarlo
            Evento evento = tipoTicketRepositorio.findEventoByTipoTicket(tipoTicket.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado para el tipo de ticket"));
            Integer edadMinima = evento.getEdadMinima();
            if (edadMinima != null && edadMinima > 0 && edadCliente != null && edadCliente < edadMinima) {
                throw new IllegalArgumentException("El evento '%s' requiere edad mínima...");
            }
            
            // Validar límite por persona
            validarLimitePorPersona(tipoTicket, itemDTO.getCantidad(), cliente);
            List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, itemDTO.getCantidad())
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
            List<Ticket> tickets = new ArrayList<>();
            for (int i = 0; i < ticketsDisponibles.size(); i++) {
                Ticket ticket = ticketsDisponibles.get(i);
                DatosAsistenteDTO asistente = itemDTO.getAsistentes().get(i);
                ticket.setEstado(EstadoTicket.RESERVADA);
                ticket.setItemCarrito(item);
                ticket.setCliente(cliente);
                ticket.setEvento(evento); // Asignar el evento al ticket
                ticket.setTipoDocumentoAsistente(asistente.getTipoDocumento());
                ticket.setDocumentoAsistente(asistente.getNumeroDocumento());
                ticket.setNombreAsistente(asistente.getNombres());
                ticket.setApellidoAsistente(asistente.getApellidos());
                String codigoQr = generarCodigoQrUnico();
                ticket.setCodigoQr(codigoQr);
                ticket.setQrImage(generarQrComoBytes(codigoQr));
                tickets.add(ticket);
            }
            item.setTickets(tickets);
            items.add(item);
            int cantidadReservada = itemDTO.getCantidad();
            tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() - cantidadReservada);
            tipoTicket.setCantidadVendida(tipoTicket.getCantidadVendida() + cantidadReservada);
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
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        orden.setEstado(EstadoCompra.APROBADO);
        Map<Evento, Integer> cantidadPorEvento = new HashMap<>();

        for (ItemCarrito item : orden.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                ticket.setEstado(EstadoTicket.VENDIDA);
            }
            // Obtener evento relacionado
            Evento evento = tipoTicketRepositorio.findEventoByTipoTicket(item.getTipoTicket().getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado para el tipo de ticket"));
            cantidadPorEvento.merge(evento, item.getCantidad(), Integer::sum);
        }
        // Actualizar aforo de los eventos involucrados
        for (Map.Entry<Evento, Integer> entry : cantidadPorEvento.entrySet()) {
            Evento evento = entry.getKey();
            Integer cantidadVendida = entry.getValue();

            if (evento.getAforoDisponible() != null) {
                evento.setAforoDisponible(Math.max(evento.getAforoDisponible() - cantidadVendida, 0));
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
    public OrdenCompra comprarDesdeCarrito(Integer idCarrito) {
        log.info("Iniciando conversión de carrito ID: {}", idCarrito);
        CarroCompras carrito = carroComprasRepository.findById(idCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado con ID: " + idCarrito));
        if (!carrito.getActivo() || carrito.getItems().isEmpty()) {
            throw new BusinessException("El carrito está inactivo o vacío y no puede ser comprado.");
        }
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(carrito.getCliente());
        orden.setFechaOrden(LocalDate.now());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        orden.setCarroCompras(carrito);
        for (ItemCarrito item : new ArrayList<>(carrito.getItems())) {
            if (item.getTickets().stream().anyMatch(t -> t.getEstado() != EstadoTicket.RESERVADA)) {
                throw new BusinessException("Error de consistencia: El item " + item.getIdItemCarrito() + " no tiene todos sus tickets reservados.");
            }
            carrito.removeItem(item);
            orden.addItem(item);
            
            // Asociar todos los tickets del item con la orden
            for (Ticket ticket : item.getTickets()) {
                ticket.setOrdenCompra(orden);
            }
        }
        orden.calcularTotal();
        carrito.setActivo(false);
        carrito.setFechaActualizacion(LocalDateTime.now());
        log.info("Guardando nueva orden desde carrito ID {} para cliente ID {}", idCarrito, carrito.getCliente().getIdPersona());
        
        OrdenCompra ordenGuardada = ordenCompraRepositorio.save(orden);
        
        // Guardar explícitamente todos los tickets
        for (ItemCarrito item : ordenGuardada.getItems()) {
            for (Ticket ticket : item.getTickets()) {
                ticketRepository.save(ticket);
            }
        }
        
        // Actualizar el historial de compras del cliente
        Cliente cliente = carrito.getCliente();
        cliente.getOrdenesCompra().add(ordenGuardada);
        clienteRepository.save(cliente);
        
        return ordenGuardada;
    }

    @Transactional
    public void registrarAsistentes(Integer idOrden, RegistrarParticipantesDTO dto) {
        log.info("Registrando asistentes para orden ID: {}", idOrden);
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + idOrden));
        if (orden.getEstado() != EstadoCompra.PENDIENTE) {
            throw new BusinessException("Solo se pueden registrar asistentes en órdenes pendientes.");
        }
        Map<Integer, Ticket> ticketsDeLaOrden = orden.getItems().stream()
                .flatMap(item -> item.getTickets().stream())
                .filter(ticket -> ticket.getEstado() == EstadoTicket.RESERVADA)
                .collect(Collectors.toMap(Ticket::getIdTicket, ticket -> ticket));
        if (dto.getParticipantes().size() != ticketsDeLaOrden.size()) {
            throw new IllegalArgumentException(
                    String.format("La cantidad de participantes enviados (%d) no coincide con los tickets reservados (%d) de la orden.",
                            dto.getParticipantes().size(), ticketsDeLaOrden.size())
            );
        }
        for (DatosAsistenteDTO participante : dto.getParticipantes()) {
            Ticket ticket = ticketsDeLaOrden.get(participante.getIdTicket());
            if (ticket == null) {
                log.warn("Se intentó registrar asistente para ticket ID {} que no pertenece o no está reservado en la orden {}",
                        participante.getIdTicket(), idOrden);
                throw new IllegalArgumentException("Ticket ID " + participante.getIdTicket() + " inválido para esta orden.");
            }
            ticket.setNombreAsistente(participante.getNombres());
            ticket.setApellidoAsistente(participante.getApellidos());
            ticket.setTipoDocumentoAsistente(participante.getTipoDocumento());
            ticket.setDocumentoAsistente(participante.getNumeroDocumento());
        }

        orden.setFechaActualizacion(LocalDate.now());
        log.info("Asistentes registrados correctamente para orden ID: {}", idOrden);
    }
}
