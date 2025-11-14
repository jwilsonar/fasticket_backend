package pe.edu.pucp.fasticket.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.dto.ItemCarritoDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.compra.ItemCarritoRepository;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarroComprasServiceImpl implements CarroComprasService {

    private final CarroComprasRepository carroComprasRepository;
    private final ClienteRepository clienteRepository;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final ItemCarritoRepository itemCarritoRepository;
    private final TicketRepository ticketRepository;
    private final FidelizacionService fidelizacionService;

    private static final int LIMITE_MAXIMO_TICKETS_POR_CLIENTE = 10;
    public static final int TIEMPO_RESERVA_MINUTOS = 15;

    @Override
    @Transactional
    public CarroComprasDTO agregarItemAlCarrito(AddItemRequestDTO request) {
        log.info("Agregando item (con reserva) al carrito para cliente ID: {}", request.getIdCliente());

        TipoTicket tipoTicket = tipoTicketRepositorio.findById(request.getIdTipoTicket())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + request.getIdTipoTicket()));

        if (Boolean.FALSE.equals(tipoTicket.getActivo())) {
            throw new BusinessException("El tipo de ticket '" + tipoTicket.getNombre() + "' no está disponible para la venta.");
        }
        Double precioActual = tipoTicket.getPrecioCalculado();
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.getIdCliente()));

        validarLimitePorPersona(tipoTicket, request.getCantidad(), cliente);

        List<Ticket> ticketsAReservar = ticketRepository.findAvailableTicketsByTypeAndState(
                tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, request.getCantidad())
        );

        if (ticketsAReservar.size() < request.getCantidad()) {
            throw new BusinessException("Stock insuficiente para " + tipoTicket.getNombre() +
                    ". Solo quedan " + ticketsAReservar.size() + " tickets reales.");
        }
        CarroCompras carro = carroComprasRepository.findByCliente_IdPersonaAndActivoTrue(cliente.getIdPersona())
                .orElseGet(() -> {
                    log.info("No se encontró NINGÚN carrito para cliente ID: {}, creando uno nuevo.", cliente.getIdPersona());
                    CarroCompras nuevoCarro = new CarroCompras();
                    nuevoCarro.setCliente(cliente);
                    nuevoCarro.setFechaCreacion(LocalDateTime.now());
                    return nuevoCarro;
                });

        if (!carro.getItems().isEmpty()) {
            Integer idEventoActual = carro.getItems().get(0).getTipoTicket().getEvento().getIdEvento();
            if (!tipoTicket.getEvento().getIdEvento().equals(idEventoActual)) {
                throw new BusinessException("No puedes añadir tickets de diferentes eventos al mismo carrito.");
            }
        } else {
            carro.setIdEventoActual(tipoTicket.getEvento().getIdEvento());
        }
        ItemCarrito itemExistente = null;
        for (ItemCarrito item : carro.getItems()) {
            if (item.getTipoTicket().getIdTipoTicket().equals(tipoTicket.getIdTipoTicket()) &&
                item.getPrecio().equals(precioActual)) {
                itemExistente = item;
                break;
            }
        }

        if (itemExistente != null) {
            log.info("Item ID {} ya existe. Añadiendo {} tickets.", itemExistente.getIdItemCarrito(), request.getCantidad());

            int nuevaCantidad = itemExistente.getCantidad() + request.getCantidad();
            validarLimitePorPersona(tipoTicket, nuevaCantidad, cliente); // Valida el total
            itemExistente.setCantidad(nuevaCantidad);
            itemExistente.calcularPrecioFinal();

            for (Ticket ticket : ticketsAReservar) {
                ticket.setEstado(EstadoTicket.RESERVADA);
                ticket.setCliente(cliente);
                ticket.setItemCarrito(itemExistente);
            }
            itemExistente.getTickets().addAll(ticketsAReservar);
            itemCarritoRepository.save(itemExistente); // Guarda el item actualizado

        } else {
            log.info("Creando nuevo ItemCarrito para TipoTicket ID {}", tipoTicket.getIdTipoTicket());

            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setTipoTicket(tipoTicket);
            nuevoItem.setCantidad(request.getCantidad());
            nuevoItem.setPrecio(precioActual);
            nuevoItem.setFechaAgregado(LocalDate.now());
            nuevoItem.setCarroCompra(carro);
            nuevoItem.calcularPrecioFinal();

            ItemCarrito itemGuardado = itemCarritoRepository.save(nuevoItem);

            for (Ticket ticket : ticketsAReservar) {
                ticket.setEstado(EstadoTicket.RESERVADA);
                ticket.setCliente(cliente);
                ticket.setItemCarrito(itemGuardado);
            }

            itemGuardado.setTickets(ticketsAReservar);
            carro.addItem(itemGuardado);
        }
        carro.recalcularTotales();
        carro.setFechaActualizacion(LocalDateTime.now().plusMinutes(TIEMPO_RESERVA_MINUTOS));
        CarroCompras carroGuardado = carroComprasRepository.save(carro);
        ticketRepository.saveAll(ticketsAReservar);
        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() - request.getCantidad());
        tipoTicketRepositorio.save(tipoTicket);
        return convertirADTO(carroGuardado);
    }

    @Transactional
    public List<Ticket> reservarTickets(TipoTicket tipoTicket, int cantidad) {
        if (tipoTicket.getCantidadDisponible() < cantidad) {
            throw new BusinessException("Stock insuficiente (contador) para el ticket: " + tipoTicket.getNombre());
        }
        List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, cantidad));
        if (ticketsDisponibles.size() < cantidad) {
            throw new BusinessException("Stock insuficiente (inventario) para el ticket: " + tipoTicket.getNombre());
        }
        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() - cantidad);

        return ticketsDisponibles;
    }

    @Override
    @Transactional
    public CarroComprasDTO eliminarTicketIndividualDelCarrito(Integer idTicket, Integer idCliente) {
        log.info("Eliminando Ticket ID: {} para Cliente ID: {}", idTicket, idCliente);

        Ticket ticket = ticketRepository.findById(idTicket)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con ID: " + idTicket));

        if (ticket.getCliente() == null || !ticket.getCliente().getIdPersona().equals(idCliente)) {
            throw new SecurityException("Acción no permitida. El ticket no pertenece a este cliente.");
        }
        if (ticket.getEstado() != EstadoTicket.RESERVADA) {
            throw new BusinessException("Solo se pueden eliminar tickets que estén RESERVADOS en el carrito.");
        }

        ItemCarrito item = ticket.getItemCarrito();
        if (item == null) {
            throw new IllegalStateException("Error de consistencia: El ticket reservado no tiene ItemCarrito.");
        }
        CarroCompras carro = item.getCarroCompra();
        TipoTicket tipoTicket = item.getTipoTicket();

        // 1. Liberar el Ticket
        ticket.setEstado(EstadoTicket.DISPONIBLE);
        ticket.setCliente(null);
        ticket.setItemCarrito(null);
        ticket.setNombreAsistente(null);
        ticket.setApellidoAsistente(null);
        ticket.setTipoDocumentoAsistente(null);
        ticket.setDocumentoAsistente(null);
        ticket.setCodigoQr(null);
        ticket.setQrImage(null);
        ticketRepository.save(ticket); // Guarda el ticket liberado

        // 2. Actualizar el stock
        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() + 1);
        tipoTicketRepositorio.save(tipoTicket);
        log.info("Liberado 1 ticket del tipo {}", tipoTicket.getNombre());

        // 3. Actualizar el ItemCarrito (Reducir cantidad o borrarlo)
        if (item.getCantidad() > 1) {
            item.setCantidad(item.getCantidad() - 1);
            item.getTickets().remove(ticket); // Quita el ticket de la lista del item
            item.calcularPrecioFinal();
            itemCarritoRepository.save(item);
        } else {
            // Si era el último ticket, borra el item
            carro.removeItem(item); // Prepara para orphanRemoval
            // itemCarritoRepository.delete(item); // No es necesario si CarroCompras tiene orphanRemoval=true
        }

        // 4. Recalcular el total del Carro (Arreglo Bug #1)
        carro.recalcularTotales();
        carroComprasRepository.save(carro);

        return convertirADTO(carro);
    }

    private void validarLimitePorPersona(TipoTicket tipoTicket, Integer cantidad, Cliente cliente) {
        if (tipoTicket.getLimitePorPersona() != null && tipoTicket.getLimitePorPersona() > 0) {
            Integer ticketsComprados = ticketRepository.countTicketsByClienteAndTipoTicket(cliente.getIdPersona(), tipoTicket.getIdTipoTicket());
            if (ticketsComprados + cantidad > tipoTicket.getLimitePorPersona()) {
                throw new BusinessException("El límite de tickets por persona para '" + tipoTicket.getNombre() + "' es de " +
                    tipoTicket.getLimitePorPersona() + ". Ya has comprado " + ticketsComprados + " tickets de este tipo.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CarroComprasDTO verCarrito(Integer idCliente) {
        return carroComprasRepository.findByCliente_IdPersonaAndActivoTrue(idCliente)
                .map(this::convertirADTO)
                .orElseGet(() -> crearCarritoVacioDTO(idCliente));
    }

    private CarroComprasDTO convertirADTO(CarroCompras carro) {
        CarroComprasDTO dto = new CarroComprasDTO();
        dto.setIdCarro(carro.getIdCarro());
        dto.setSubtotal(carro.getSubtotal());
        dto.setTotal(carro.getTotal());

        dto.setItems(carro.getItems().stream().map(item -> {
            ItemCarritoDTO itemDTO = new ItemCarritoDTO();
            itemDTO.setIdItemCarrito(item.getIdItemCarrito());
            itemDTO.setCantidad(item.getCantidad());

            if (item.getTipoTicket() != null) {
                itemDTO.setIdTipoTicket(item.getTipoTicket().getIdTipoTicket());
                itemDTO.setNombreTicket(item.getTipoTicket().getNombre());
                itemDTO.setPrecioUnitario(item.getPrecio());
                itemDTO.setSubtotal(item.getPrecio() * item.getCantidad());
            }
            return itemDTO;
        }).collect(Collectors.toList()));

        return dto;
    }

    private CarroComprasDTO crearCarritoVacioDTO(Integer idCliente) {
        CarroComprasDTO dto = new CarroComprasDTO();
        dto.setIdCarro(null);
        dto.setSubtotal(0.0);
        dto.setTotal(0.0);
        dto.setItems(Collections.emptyList());
        return dto;
    }

    @Override
    @Transactional
    public CarroComprasDTO aplicarCodigoPromocional(Integer idCarrito, String codigo) {
        log.info("Aplicando código {} al carrito ID: {}", codigo, idCarrito);

        CarroCompras carro = carroComprasRepository.findById(idCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado: " + idCarrito));

        if (carro.getCodigoPromocionalAplicado() != null) {
            throw new BusinessException("Ya se ha aplicado un código a este carrito.");
        }
        Double descuento = fidelizacionService.validarYCalcularDescuento(codigo, carro.getSubtotal());
        carro.setCodigoPromocionalAplicado(codigo);
        carro.setDescuentoPromocional(descuento);
        carro.recalcularTotales();

        CarroCompras carroGuardado = carroComprasRepository.save(carro);
        return convertirADTO(carroGuardado);
    }
}