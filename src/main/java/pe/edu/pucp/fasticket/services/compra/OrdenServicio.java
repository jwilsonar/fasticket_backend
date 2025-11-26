package pe.edu.pucp.fasticket.services.compra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.FetchProfile.Item;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.compra.AsistenteParaItemDTO;
import pe.edu.pucp.fasticket.dto.compra.BeneficiosDTO;
import pe.edu.pucp.fasticket.dto.compra.CheckoutCarritoRequestDTO;
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemSeleccionadoDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemsDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.ProcesarCompraResponseDTO;
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
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.compra.ItemCarritoRepository;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;
import pe.edu.pucp.fasticket.model.fidelizacion.CodigoPromocional;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;

import static pe.edu.pucp.fasticket.services.CarroComprasServiceImpl.TIEMPO_RESERVA_MINUTOS;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import static pe.edu.pucp.fasticket.services.CarroComprasServiceImpl.TIEMPO_RESERVA_MINUTOS;
import pe.edu.pucp.fasticket.services.EmailService;
import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;


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

    private final AuditLogService auditLogService;
    private final AdministradorRepository administradorRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final EmailService emailService;
    @PersistenceContext
    private EntityManager entityManager;

    public OrdenServicio(
            OrdenCompraRepositorio ordenCompraRepositorio,
            TipoTicketRepositorio tipoTicketRepositorio,
            ClienteRepository clienteRepository,
            TicketRepository ticketRepository,
            ApplicationEventPublisher eventPublisher,
            ItemCarritoRepository itemCarritoRepositorio,
            CarroComprasRepository carroComprasRepository,
            FidelizacionService fidelizacionService,

            AuditLogService auditLogService,
            AdministradorRepository administradorRepository,
            ConfiguracionRepository configuracionRepository,
            EmailService emailService
    ) {
        this.ordenCompraRepositorio = ordenCompraRepositorio;
        this.tipoTicketRepositorio = tipoTicketRepositorio;
        this.clienteRepository = clienteRepository;
        this.ticketRepository = ticketRepository;
        this.itemCarritoRepositorio = itemCarritoRepositorio;
        this.carroComprasRepository = carroComprasRepository;
        this.fidelizacionService = fidelizacionService;

        this.auditLogService = auditLogService;
        this.administradorRepository = administradorRepository;
        this.configuracionRepository = configuracionRepository;
        this.emailService = emailService;
    }

    @Transactional
    public OrdenCompra crearOrden(CrearOrdenDTO datosOrden, Integer idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + idCliente));

        CarroCompras carrito = carroComprasRepository
                .findByCliente_IdPersonaAndActivoTrue(cliente.getIdPersona())
                .orElse(null);

        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(cliente);
        orden.setFechaOrden(LocalDate.now());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        orden.setActivo(true);
        orden.setRuc(datosOrden.getRuc());
        orden.setRazonSocial(datosOrden.getRazonSocial());
        orden.setDireccionFiscal(datosOrden.getDireccionFiscal());
        orden.setCodigoSeguimiento(java.util.UUID.randomUUID().toString());
        orden.setItems(new ArrayList<>());
        OrdenCompra ordenGuardada = ordenCompraRepositorio.saveAndFlush(orden);
        log.info("Orden ID: {} creada para cliente ID: {}", ordenGuardada.getIdOrdenCompra(), idCliente);
        boolean tieneItemsEnCarrito = (carrito != null && !carrito.getItems().isEmpty());
        if (tieneItemsEnCarrito) {
            log.info("Procesando items del carrito ID {}", carrito.getIdCarro());
            List<Integer> itemIds = carrito.getItems().stream()
                    .map(ItemCarrito::getIdItemCarrito)
                    .collect(Collectors.toList());
            for (Integer itemId : itemIds) {
                ItemCarrito item = itemCarritoRepositorio.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado: " + itemId));
                validarStockItem(item);
            }
            for (Integer itemId : itemIds) {
                ItemCarrito item = itemCarritoRepositorio.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado: " + itemId));
                itemCarritoRepositorio.transferirItemAOrden(itemId, ordenGuardada);

                ItemCarrito itemActualizado = itemCarritoRepositorio.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado después de transferir: " + itemId));

                actualizarTicketsConAsistentes(itemActualizado, datosOrden, ordenGuardada);

                log.debug("Item ID {} transferido del carrito a la orden {}", itemId, ordenGuardada.getIdOrdenCompra());
            }
            carroComprasRepository.desactivarCarrito(carrito.getIdCarro());

        } else {
            log.info("Creando items nuevos (compra directa)");

            validarLimitePorCompra(datosOrden.getItems());
            validarLimitesPorPersona(datosOrden.getItems(), cliente);
            validarStockDisponible(datosOrden.getItems());

            List<ItemCarrito> itemsNuevos = construirYGuardarItems(datosOrden.getItems(), cliente, ordenGuardada);
            ordenGuardada.getItems().addAll(itemsNuevos);
        }

        ordenCompraRepositorio.flush();

        ordenGuardada = ordenCompraRepositorio.findById(ordenGuardada.getIdOrdenCompra())
                .orElseThrow(() -> new ResourceNotFoundException("Error al recargar orden"));

        log.debug("DEBUG DESPUÉS DE RECARGAR - Items en memoria: {}", ordenGuardada.getItems().size());
        List<ItemCarrito> itemsDeLaOrden = itemCarritoRepositorio.findByOrdenCompra_IdOrdenCompra(ordenGuardada.getIdOrdenCompra());

        if (itemsDeLaOrden.isEmpty()) {
            log.error("ERROR CRÍTICO: No se encontraron items para la orden {}", ordenGuardada.getIdOrdenCompra());
            throw new BusinessException("No se pudieron cargar los items de la orden");
        }

        log.info("Items cargados para orden {}: {} items encontrados",
                ordenGuardada.getIdOrdenCompra(), itemsDeLaOrden.size());

        ordenGuardada.setItems(itemsDeLaOrden);
        double subtotalCalculado = itemsDeLaOrden.stream()
                .mapToDouble(item -> {
                    double precio = item.getPrecio() != null ? item.getPrecio() : 0.0;
                    int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;
                    double lineTotal = precio * cantidad;
                    log.debug("Item: {} x {} = {}", item.getTipoTicket().getNombre(), cantidad, lineTotal);
                    return lineTotal;
                })
                .sum();

        ordenGuardada.setSubtotal(Math.round(subtotalCalculado * 100.0) / 100.0);

        double subtotalInicial = ordenGuardada.getSubtotal() != null ? ordenGuardada.getSubtotal() : 0.0;
        log.info("SUBTOTAL calculado: {} (suma de precioFinal de items)", subtotalInicial);
        String nivelCliente = cliente.getNivel() != null ? cliente.getNivel().toString() : "SIN_NIVEL";
        log.info("Cliente nivel: {}", nivelCliente);

        calcularDescuentoPorMembresia(ordenGuardada, cliente);

        double descuento = ordenGuardada.getDescuentoPorMembrecia() != null ? ordenGuardada.getDescuentoPorMembrecia() : 0.0;
        log.info("DESCUENTO aplicado: {} soles", descuento);
        ordenGuardada.calcularTotal();

        double totalFinal = ordenGuardada.getTotal() != null ? ordenGuardada.getTotal() : 0.0;
        double igvFinal = ordenGuardada.getIgv() != null ? ordenGuardada.getIgv() : 0.0;

        log.info("TOTAL FINAL: {} (subtotal {} - descuento {})", totalFinal, subtotalInicial, descuento);
        log.info("IGV calculado: {}", igvFinal);

        OrdenCompra ordenFinal = ordenCompraRepositorio.saveAndFlush(ordenGuardada);

        log.info("Orden ID {} COMPLETADA | Items: {} | Subtotal: {} | Descuento: {} | Total: {} | IGV: {}",
                ordenFinal.getIdOrdenCompra(),
                ordenFinal.getItems().size(),
                ordenFinal.getSubtotal(),
                ordenFinal.getDescuentoPorMembrecia(),
                ordenFinal.getTotal(),
                ordenFinal.getIgv());

        return ordenFinal;
    }

    private void actualizarTicketsConAsistentes(ItemCarrito item, CrearOrdenDTO datosOrden, OrdenCompra orden) {
        // Buscar el DTO que corresponde a este item
        ItemSeleccionadoDTO dtoCoincidente = datosOrden.getItems().stream()
                .filter(d -> d.getIdTipoTicket().equals(item.getTipoTicket().getIdTipoTicket()))
                .findFirst()
                .orElse(null);

        // Obtener los IDs de tickets asociados al item
        List<Integer> ticketIds = ticketRepository.findTicketIdsByItemCarritoId(item.getIdItemCarrito());
        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);

            // Actualizar estado y orden
            ticket.setEstado(EstadoTicket.RESERVADA);
            ticket.setOrdenCompra(orden);

            // Asignar datos de asistente si están disponibles
            if (dtoCoincidente != null && dtoCoincidente.getAsistentes() != null
                    && i < dtoCoincidente.getAsistentes().size()) {

                DatosAsistenteDTO datosAsistente = dtoCoincidente.getAsistentes().get(i);
                ticket.setNombreAsistente(datosAsistente.getNombres());
                ticket.setApellidoAsistente(datosAsistente.getApellidos());
                ticket.setDocumentoAsistente(datosAsistente.getNumeroDocumento());
                if (datosAsistente.getTipoDocumento() != null) {
                    try {
                        ticket.setTipoDocumentoAsistente(datosAsistente.getTipoDocumento());
                    } catch (IllegalArgumentException e) {
                        log.warn("Tipo de documento inválido '{}', usando DNI por defecto",
                                datosAsistente.getTipoDocumento());
                        ticket.setTipoDocumentoAsistente(
                                pe.edu.pucp.fasticket.model.usuario.TipoDocumento.DNI
                        );
                    }
                }
                if (ticket.getCodigoQr() == null || ticket.getQrImage() == null) {
                    String codigoQr = generarCodigoQrUnico();
                    ticket.setCodigoQr(codigoQr);
                    ticket.setQrImage(generarQrComoBytes(codigoQr));
                }
            }

            ticketRepository.save(ticket);
        }
    }

    /**
     * Validar que el item tenga stock suficiente antes de procesarlo.
     */
    private void validarStockItem(ItemCarrito item) {
        TipoTicket tipoTicket = item.getTipoTicket();

        if (Boolean.FALSE.equals(tipoTicket.getActivo())) {
            throw new BusinessException("La venta de '" + tipoTicket.getNombre() + "' está pausada.");
        }

        Integer cantidadDisponible = tipoTicket.getCantidadDisponible();
        if (cantidadDisponible == null || cantidadDisponible < item.getCantidad()) {
            throw new BusinessException("Stock insuficiente para '" + tipoTicket.getNombre() +
                    "'. Disponible: " + (cantidadDisponible != null ? cantidadDisponible : 0) +
                    ", Solicitado: " + item.getCantidad());
        }
    }

    /**
     * Calcula el descuento según el nivel de membresía del cliente.
     */

    @Transactional
    public OrdenCompra crearOrden(CrearOrdenDTO datosOrden) {
        if (datosOrden == null || datosOrden.getIdCliente() == null) {
            throw new ResourceNotFoundException("Cliente no encontrado: idCliente nulo");
        }
        return crearOrden(datosOrden, datosOrden.getIdCliente());
    }

    // --- NUEVO MÉTODO HELPER (RF-046) ---
    public void validarLimitePorCompra(List<ItemSeleccionadoDTO> itemsDTO) {
        int totalTicketsEnOrden = itemsDTO.stream()
                .mapToInt(item -> item.getCantidad() != null ? item.getCantidad() : 0)
                .sum();

        // Leer el límite desde la BD (RF-046)
        int limitePorCompra = configuracionRepository.findById("LIMITE_TICKETS_POR_COMPRA")
                .map(config -> Integer.parseInt(config.getValue()))
                .orElse(5); // Valor por defecto si no se encuentra

        if (totalTicketsEnOrden > limitePorCompra) {
            throw new BusinessException("No puede comprar más de " + limitePorCompra + " tickets por orden.");
        }
    }

    public void validarLimitesPorPersona(List<ItemSeleccionadoDTO> itemsDTO, Cliente cliente) {
        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + itemDTO.getIdTipoTicket()));

            // VALIDACIÓN CLAVE (RF-028 + RF-084)
            if (Boolean.FALSE.equals(tipoTicket.getActivo())) {
                throw new BusinessException("La venta de esta categoría de ticket está pausada temporalmente.");
            }

            validarLimitePorPersona(tipoTicket, itemDTO.getCantidad(), cliente);
        }
    }



    public void validarStockDisponible(List<ItemSeleccionadoDTO> itemsDTO) {
        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + itemDTO.getIdTipoTicket()));
            log.info("Tipo de ticket: {}", tipoTicket);
            // Validar que el tipo de ticket esté activo
            if (Boolean.FALSE.equals(tipoTicket.getActivo())) {
                throw new BusinessException("El tipo de ticket '" + tipoTicket.getNombre() + "' no está disponible para la venta.");
            }

            // Validar el contador de cantidad disponible (evitar NPEs y mensajes genéricos)
            Integer cantidadDisponible = tipoTicket.getCantidadDisponible();
            Integer cantidadSolicitada = itemDTO.getCantidad() != null ? itemDTO.getCantidad() : 0;
            if (cantidadDisponible == null || cantidadDisponible < cantidadSolicitada) {
                throw new BusinessException("No hay suficientes tickets disponibles");
            }

            // Validar que existan suficientes tickets en estado DISPONIBLE en la BD
            List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, itemDTO.getCantidad())
            );

            if (ticketsDisponibles.size() < itemDTO.getCantidad()) {
                throw new BusinessException("No hay suficientes tickets disponibles");
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
        double subtotalBase = orden.getSubtotal() != null ? orden.getSubtotal() : 0.0;
        double descuentoPromocional = orden.getDescuentoPromocional() != null ? orden.getDescuentoPromocional() : 0.0;

        double subtotalDescontado = Math.max(subtotalBase - descuentoPromocional, 0.0);
        double descuentoMembresia = subtotalDescontado * porcentajeDescuento;
        orden.setDescuentoPorMembrecia(descuentoMembresia);
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


    public List<ItemCarrito> construirYGuardarItems(List<ItemSeleccionadoDTO> itemsDTO,
                                                     Cliente cliente, OrdenCompra orden) {

        List<ItemCarrito> itemsGuardados = new ArrayList<>();

        for (ItemSeleccionadoDTO itemDTO : itemsDTO) {
            // Recargar el TipoTicket para obtener el estado más reciente (evitar condiciones de carrera)
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(itemDTO.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + itemDTO.getIdTipoTicket()));
            
            validarLimitePorPersona(tipoTicket, itemDTO.getCantidad(), cliente);

            // Validar stock justo antes de reservar (evitar condiciones de carrera)
            if (Boolean.FALSE.equals(tipoTicket.getActivo())) {
                throw new BusinessException("El tipo de ticket '" + tipoTicket.getNombre() + "' no está disponible para la venta.");
            }

            Integer cantidadDisponible = tipoTicket.getCantidadDisponible();
            Integer cantidadSolicitada = itemDTO.getCantidad() != null ? itemDTO.getCantidad() : 0;
            if (cantidadDisponible == null || cantidadDisponible < cantidadSolicitada) {
                throw new BusinessException("No hay suficientes tickets disponibles");
            }

            ItemCarrito item = new ItemCarrito();
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecio(tipoTicket.getPrecioCalculado());
            item.setFechaAgregado(LocalDate.now());
            item.setTipoTicket(tipoTicket);
            item.setOrdenCompra(orden);
            item.calcularPrecioFinal();

            ItemCarrito itemGuardado = itemCarritoRepositorio.save(item);

            // Validar que existan suficientes tickets en estado DISPONIBLE y activos
            List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, itemDTO.getCantidad())
            );

            if (ticketsDisponibles.size() < itemDTO.getCantidad()) {
                throw new BusinessException("Stock insuficiente (inventario) para '" + tipoTicket.getNombre() + 
                        "'. Solicitados: " + itemDTO.getCantidad() + 
                        ", Disponibles en BD: " + ticketsDisponibles.size());
            }

            List<Ticket> ticketsReservados = new ArrayList<>();

            for (int i = 0; i < itemDTO.getCantidad(); i++) {

                Ticket ticket = ticketsDisponibles.get(i);
                ticket.setEstado(EstadoTicket.RESERVADA);
                ticket.setCliente(cliente);

                Evento evento = tipoTicketRepositorio.findEventoByTipoTicket(tipoTicket.getIdTipoTicket())
                        .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

                ticket.setEvento(evento);
                ticket.setItemCarrito(itemGuardado);
                ticket.setOrdenCompra(orden);

                // Asignar asistentes si vienen en el DTO (para tests/unit)
                if (itemDTO.getAsistentes() != null && i < itemDTO.getAsistentes().size()) {
                    DatosAsistenteDTO asistente = itemDTO.getAsistentes().get(i);
                    ticket.setNombreAsistente(asistente.getNombres());
                    ticket.setApellidoAsistente(asistente.getApellidos());
                    ticket.setTipoDocumentoAsistente(asistente.getTipoDocumento());
                    ticket.setDocumentoAsistente(asistente.getNumeroDocumento());
                }

                String codigoQr = generarCodigoQrUnico();
                ticket.setCodigoQr(codigoQr);
                ticket.setQrImage(generarQrComoBytes(codigoQr));

                ticketsReservados.add(ticket);
            }

            ticketRepository.saveAll(ticketsReservados);
            itemGuardado.setTickets(ticketsReservados);

            itemsGuardados.add(itemGuardado);
            
            // Actualizar contadores del TipoTicket de forma atómica
            int cantidadReservada = itemDTO.getCantidad() != null ? itemDTO.getCantidad() : 0;
            int disponibleActual = tipoTicket.getCantidadDisponible() != null ? tipoTicket.getCantidadDisponible() : 0;
            int vendidaActual = tipoTicket.getCantidadVendida() != null ? tipoTicket.getCantidadVendida() : 0;
            int nuevaCantidadDisponible = disponibleActual - cantidadReservada;
            int nuevaCantidadVendida = vendidaActual + cantidadReservada;
            
            tipoTicket.setCantidadDisponible(Math.max(nuevaCantidadDisponible, 0)); // Evitar valores negativos
            tipoTicket.setCantidadVendida(nuevaCantidadVendida);
            
            // Guardar el TipoTicket actualizado
            tipoTicketRepositorio.save(tipoTicket);
            log.debug("Stock actualizado para TipoTicket ID {}: Disponible={}, Vendida={}", 
                    tipoTicket.getIdTipoTicket(), tipoTicket.getCantidadDisponible(), tipoTicket.getCantidadVendida());
        }

        return itemsGuardados;
    }


    public OrdenResumenDTO generarResumenOrden(CrearOrdenDTO datosOrden) {
        // Este método genera un resumen previo a la creación de la orden
        // Nota: Para obtener un resumen completo de una orden ya creada, usar el constructor de OrdenResumenDTO con OrdenCompra
        List<ItemResumenDTO> resumenItems = new ArrayList<>();
        double subtotal = 0.0;

        for (ItemSeleccionadoDTO item : datosOrden.getItems()) {
            if (item.getIdTipoTicket() == null) {
                continue;
            }
            TipoTicket tipoTicket = tipoTicketRepositorio.findById(item.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con id: " + item.getIdTipoTicket()));
            ItemResumenDTO itemResumen = new ItemResumenDTO();
            itemResumen.setNombreTipoTicket(tipoTicket.getNombre());
            itemResumen.setCantidad(item.getCantidad() != null ? item.getCantidad() : 0);
            double precioActual = tipoTicket.getPrecioCalculado();
            itemResumen.setPrecioUnitario(precioActual);
            int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;
            subtotal += tipoTicket.getPrecio() * cantidad;
            resumenItems.add(itemResumen);
        }
        
        // Crear una orden temporal para generar el resumen completo
        // Nota: Este método está deprecado, se recomienda crear la orden primero y luego generar el resumen
        OrdenCompra ordenTemporal = new OrdenCompra();
        ordenTemporal.setEstado(EstadoCompra.PENDIENTE); // Evitar NPE en mapeos que usan estado
        ordenTemporal.setSubtotal(subtotal);
        ordenTemporal.setTotal(subtotal);
        OrdenResumenDTO resumen = new OrdenResumenDTO(ordenTemporal);
        resumen.setItems(resumenItems);
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
        if (carrito != null) {
            List<OrdenCompra> otrasOrdenesActivas = ordenCompraRepositorio
                    .findByCarroComprasIdCarroAndActivoTrue(carrito.getIdCarro());
            for (OrdenCompra o : otrasOrdenesActivas) {
                if (!o.getIdOrdenCompra().equals(idOrden)) {
                    o.setActivo(false);
                    o.setEstado(EstadoCompra.ANULADO);
                    ordenCompraRepositorio.save(o);
                    log.warn("Orden ID {} del carrito {} marcada como ANULADA por conflicto de confirmación.",
                            o.getIdOrdenCompra(), carrito.getIdCarro());
                }
            }
            carrito.setActivo(false);
            carrito.setFechaActualizacion(LocalDateTime.now());
            carroComprasRepository.save(carrito);
            log.info("Carrito ID {} marcado como INACTIVO (histórico).", carrito.getIdCarro());
            CarroCompras nuevoCarro = new CarroCompras();
            nuevoCarro.setCliente(orden.getCliente());
            nuevoCarro.setActivo(true);
            nuevoCarro.setFechaCreacion(LocalDateTime.now());
            nuevoCarro.setFechaActualizacion(LocalDateTime.now());
            nuevoCarro.setSubtotal(0.0);
            nuevoCarro.setTotal(0.0);
            carroComprasRepository.save(nuevoCarro);
            log.info("Nuevo carrito ID {} creado para cliente ID {}.", nuevoCarro.getIdCarro(),
                    nuevoCarro.getCliente() != null ? nuevoCarro.getCliente().getIdPersona() : "N/A");
        } else {
            log.info("Orden ID {} no tiene carrito asociado (compra directa).", idOrden);
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
            log.info("Enviando correo de confirmación de compra para orden ID: {}", idOrden);
            emailService.enviarCorreoConfirmacionCompra(orden);
        } catch (Exception e) {
            log.error("Error al enviar correo de confirmación (no crítico): {}", e.getMessage());
        }
        try {
            // Calculamos puntos basados en el total de la orden
            fidelizacionService.generarPuntosPorCompra(
                    orden.getCliente().getIdPersona(),
                    orden.getTotal(),
                    orden.getIdOrdenCompra()
            );
        } catch (Exception e) {
            log.error("Error no bloqueante al generar puntos: {}", e.getMessage());
        }

        log.info("Puntos generados para cliente ID {} (orden {}).",
                orden.getCliente().getIdPersona(), idOrden);
    }

    /**
     * Este método es para la lógica de negocio cuando un PAGO es RECHAZADO.
     * (RF-090 se cumple aquí: Revertir cupos)
     * Generalmente es llamado por el sistema, no un admin.
     */
    @Transactional
    public void cancelarOrden(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden).orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // Evitar doble cancelación
        if (orden.getEstado() == EstadoCompra.RECHAZADO || orden.getEstado() == EstadoCompra.ANULADO) {
            log.warn("Se intentó cancelar una orden (ID: {}) que ya estaba cancelada.", idOrden);
            return;
        }

        orden.setEstado(EstadoCompra.RECHAZADO);

        revertirStockDeOrden(orden);

        ordenCompraRepositorio.save(orden);
        log.info("Orden ID: {} marcada como RECHAZADA (pago fallido o expirada). Stock revertido.", idOrden);
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

    // Método eliminado: ya no se validan asistentes en la creación de la orden
    // Los datos de asistentes se pueden asignar posteriormente si es necesario
    
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
    public OrdenCompra checkoutDesdeCarrito(Integer idCarrito, CheckoutCarritoRequestDTO requestDTO) {
        CarroCompras carrito = carroComprasRepository.findById(idCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (!carrito.getActivo() || carrito.getItems().isEmpty()) {
            throw new BusinessException("El carrito está inactivo o vacío.");
        }
        List<AsistenteParaItemDTO> itemsConAsistentes = requestDTO.getItemsConAsistentes();

        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(carrito.getCliente());
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaExpiracion(LocalDateTime.now().plusMinutes(TIEMPO_RESERVA_MINUTOS));
        orden.setCarroCompras(carrito);

        // --- INICIO RF-081: GUARDAR DATOS DE FACTURACIÓN (Opcional) ---
        orden.setRuc(requestDTO.getRuc());
        orden.setRazonSocial(requestDTO.getRazonSocial());
        orden.setDireccionFiscal(requestDTO.getDireccionFiscal());
        // --- FIN RF-081 ---

        asignarAsistentesATicketsDelCarrito(carrito, itemsConAsistentes, orden);

        for (ItemCarrito item : new ArrayList<>(carrito.getItems())) {
            carrito.removeItem(item);
            orden.addItem(item);
        }

        orden.calcularTotal();
        orden.setCodigoPromocionalAplicado(carrito.getCodigoPromocionalAplicado());
        orden.setDescuentoPromocional(carrito.getDescuentoPromocional());

        calcularDescuentoPorMembresia(orden, carrito.getCliente());
        orden.aplicarDescuentoYRecalcular();

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

    /**
     * NUEVO MÉTODO PARA RF-089 y RF-109
     * Permite a un ADMINISTRADOR anular una compra APROBADA.
     * Esto también revierte el stock (RF-090).
     */
    @Transactional
    public void anularCompraAdmin(Integer idOrden) {
        log.info("Anulación administrativa de la orden ID: {}", idOrden);

        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + idOrden));

        if (orden.getEstado() != EstadoCompra.APROBADO) {
            throw new BusinessException("Solo se pueden anular compras que ya están APROBADAS. " +
                    "El estado actual es: " + orden.getEstado());
        }

        // 1. Cambiar estado de la orden
        orden.setEstado(EstadoCompra.ANULADO);
        orden.setActivo(false);

        // 2. Revertir Tickets (Stock y Limpieza)
        revertirStockDeOrden(orden);

        // 3. Revertir Puntos
        try {
            fidelizacionService.revertirPuntosPorAnulacion(orden);
        } catch (Exception e) {
            log.error("Error al revertir puntos de la orden {}: {}", idOrden, e.getMessage());
            // No detenemos la anulación, pero queda el log
        }

        // 4. Guardar la orden anulada
        ordenCompraRepositorio.save(orden);

        // --- INICIO AUDITORÍA ---
        try {
            Administrador admin = getAdminActual();
            String detalle = "Se ANULÓ la orden ID: " + idOrden +
                    ". Cliente Afectado: " + orden.getCliente().getEmail();
            auditLogService.registrarAuditoria(admin, "ANULAR_COMPRA", "OrdenServicio", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (ANULAR_COMPRA): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Orden ID: {} ANULADA exitosamente por un administrador.", idOrden);

        // TODO: Enviar correo de notificación al cliente sobre la anulación (RF-045)
    }

    private void revertirStockDeOrden(OrdenCompra orden) {
        log.info("Revirtiendo stock y limpiando datos para Orden ID: {}", orden.getIdOrdenCompra());

        for (ItemCarrito item : orden.getItems()) {
            // 1. Limpiar cada ticket individualmente
            for (Ticket ticket : item.getTickets()) {
                ticket.setEstado(EstadoTicket.DISPONIBLE);
                ticket.setCliente(null); // Desvincular del comprador
                ticket.setOrdenCompra(null); // Desvincular de la orden
                ticket.setItemCarrito(null);// Opcional: dependerá si quieremos mantener historial del carrito

                // IMPORTANTE: Borrar datos del asistente para proteger privacidad y evitar errores
                ticket.setNombreAsistente(null);
                ticket.setApellidoAsistente(null);
                ticket.setDocumentoAsistente(null);
                ticket.setTipoDocumentoAsistente(null);

                // Resetear QR y transferencias
                ticket.setCodigoQr(null);
                ticket.setQrImage(null);
                ticket.setContadorTransferencias(0);
                ticket.setFechaUltimaTransferencia(null);
            }

            // 2. Actualizar contadores del TipoTicket
            TipoTicket tipo = item.getTipoTicket();
            int disponibleActual = tipo.getCantidadDisponible() != null ? tipo.getCantidadDisponible() : 0;
            int vendidaActual = tipo.getCantidadVendida() != null ? tipo.getCantidadVendida() : 0;
            int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;

            tipo.setCantidadDisponible(disponibleActual + cantidad);
            tipo.setCantidadVendida(Math.max(vendidaActual - cantidad, 0));

            // Guardamos los cambios
            ticketRepository.saveAll(item.getTickets()); // Guardamos tickets limpios
            tipoTicketRepositorio.save(tipo);
        }
    }

    /**
     * Obtiene la entidad Administrador basada en el usuario actualmente logueado.
     * @return El Administrador logueado.
     * @throws ResourceNotFoundException si no se encuentra el admin en la BD o no hay sesión.
     */
    private Administrador getAdminActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No hay un usuario autenticado para la auditoría.");
        }
        String username = authentication.getName();
        return administradorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado para auditoría con username: " + username));
    }

    // Helper para crear una lista de duplas a partir de DTOs seleccionados

    @Transactional
    private ProcesarCompraResponseDTO procesarOrdenCompra(OrdenCompra orden,boolean esCanjeable, List<CodigoPromocional> codigosPromocionales) {
        // Lógica para procesar la orden de compra y verificar los beneficios aplicados 
        ProcesarCompraResponseDTO responseDTO = new ProcesarCompraResponseDTO(); // incluye los items y beneficios
        
        List<ItemsDTO> itemsDTOList = new ArrayList<>();// [ {idTipoTicket, cantidad}, ... ]
        BeneficiosDTO beneficios = new BeneficiosDTO();// [ esCanjeable, codigosPromocionales]

        List<ItemCarrito> items = orden.getItems();

        for (ItemCarrito item : items) {
            // Aquí se puede agregar lógica para verificar beneficios específicos por ítem
            ItemsDTO itemsDTO = new ItemsDTO(item.getTipoTicket().getIdTipoTicket(), item.getCantidad());
            itemsDTOList.add(itemsDTO);
        }

        List<Integer> idCodigosPromocionales = new ArrayList<>();
        for (CodigoPromocional codigo : codigosPromocionales) {
            idCodigosPromocionales.add(codigo.getIdCodigoPromocional());
        }


        // Normalizar y proteger la lista de códigos promocionales; evitar nulls y permitir lectura segura
        if (esCanjeable) {
            beneficios.setEsCanjeable(true);
            beneficios.setIdCodigosPromocionales(java.util.Collections.emptyList());
        } else {
            beneficios.setEsCanjeable(false);
            List<Integer> validos = (idCodigosPromocionales == null)
                ? new ArrayList<>()
                : idCodigosPromocionales.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
            beneficios.setIdCodigosPromocionales(java.util.Collections.unmodifiableList(validos));
        }
            
        return new ProcesarCompraResponseDTO(itemsDTOList, beneficios);
    }
    
    /**
     * Lista todas las órdenes de un cliente específico.
     * @param idCliente ID del cliente
     * @return Lista de órdenes del cliente
     */
    public List<OrdenCompra> listarOrdenesPorCliente(Integer idCliente) {
        log.info("Listando órdenes para cliente ID: {}", idCliente);
        return ordenCompraRepositorio.findByCliente_IdPersonaOrderByFechaOrdenDesc(idCliente);
    }

    /**
     * Lista todas las órdenes (solo para administradores).
     * @return Lista de todas las órdenes
     */
    public List<OrdenCompra> listarTodasLasOrdenes() {
        log.info("Listando todas las órdenes (admin)");
        return ordenCompraRepositorio.findAllByOrderByFechaOrdenDesc();
    }

    /**
     * Lista órdenes por estado.
     * @param estado Estado de la orden
     * @return Lista de órdenes con el estado especificado
     */
    public List<OrdenCompra> listarOrdenesPorEstado(EstadoCompra estado) {
        log.info("Listando órdenes con estado: {}", estado);
        return ordenCompraRepositorio.findByEstado(estado);
    }

    /**
     * Lista órdenes de un cliente por estado.
     * @param idCliente ID del cliente
     * @param estado Estado de la orden
     * @return Lista de órdenes del cliente con el estado especificado
     */
    public List<OrdenCompra> listarOrdenesPorClienteYEstado(Integer idCliente, EstadoCompra estado) {
        log.info("Listando órdenes para cliente ID: {} con estado: {}", idCliente, estado);
        return ordenCompraRepositorio.findByCliente_IdPersonaAndEstado(idCliente, estado);
    }

}
