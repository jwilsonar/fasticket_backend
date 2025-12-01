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
import pe.edu.pucp.fasticket.dto.eventos.EventoResumenDTO;
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
        List<AddItemRequestDTO.ItemRequest> itemsAProcesar = request.getItemsNormalizados();

        log.info("Agregando {} item(s) al carrito para cliente ID: {}",
                itemsAProcesar.size(), request.getIdCliente());

        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.getIdCliente()));

        CarroCompras carro = carroComprasRepository.findByCliente_IdPersonaAndActivoTrue(cliente.getIdPersona())
                .orElseGet(() -> {
                    log.info("No se encontró carrito para cliente ID: {}, creando uno nuevo.", cliente.getIdPersona());
                    CarroCompras nuevoCarro = new CarroCompras();
                    nuevoCarro.setCliente(cliente);
                    nuevoCarro.setFechaCreacion(LocalDateTime.now());
                    return nuevoCarro;
                });

        Integer idEventoCarrito = carro.getIdEventoActual();
        if (idEventoCarrito == null && !carro.getItems().isEmpty()) {
            idEventoCarrito = carro.getItems().get(0).getTipoTicket().getEvento().getIdEvento();
        }

        List<TipoTicket> tiposTicket = new ArrayList<>();
        for (AddItemRequestDTO.ItemRequest itemReq : itemsAProcesar) {
            TipoTicket tipo = tipoTicketRepositorio.findById(itemReq.getIdTipoTicket())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tipo de ticket no encontrado: " + itemReq.getIdTipoTicket()));

            Integer idEventoActual = tipo.getEvento().getIdEvento();
            if (idEventoCarrito == null) {
                idEventoCarrito = idEventoActual;
            } else if (!idEventoCarrito.equals(idEventoActual)) {
                throw new BusinessException("No puedes añadir tickets de diferentes eventos al mismo carrito.");
            }

            tiposTicket.add(tipo);
        }

        if (carro.getIdEventoActual() == null) {
            carro.setIdEventoActual(idEventoCarrito);
        }

        if (carro.getIdCarro() == null) {
            carro = carroComprasRepository.save(carro);
            log.info("Carrito nuevo guardado con ID: {}", carro.getIdCarro());
        }

        for (int i = 0; i < itemsAProcesar.size(); i++) {
            AddItemRequestDTO.ItemRequest itemReq = itemsAProcesar.get(i);
            TipoTicket tipoTicket = tiposTicket.get(i);

            log.info("Procesando item {}/{}: TipoTicket ID {} '{}', Cantidad {}",
                    i + 1, itemsAProcesar.size(),
                    tipoTicket.getIdTipoTicket(), tipoTicket.getNombre(), itemReq.getCantidad());

            validarDisponibilidadTipoTicket(tipoTicket);

            if (tipoTicket.getEvento() != null) {
                validarEdadClienteParaEvento(tipoTicket.getEvento(), cliente);
            }

            Double precioActual = tipoTicket.getPrecioCalculado();

            validarLimitePorPersona(tipoTicket, itemReq.getCantidad(), cliente);

            List<Ticket> ticketsAReservar = ticketRepository.findAvailableTicketsByTypeAndState(
                    tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, itemReq.getCantidad())
            );

            if (ticketsAReservar.size() < itemReq.getCantidad()) {
                throw new BusinessException("Stock insuficiente para " + tipoTicket.getNombre() +
                        ". Solo quedan " + ticketsAReservar.size() + " tickets disponibles.");
            }

            ItemCarrito itemExistente = carro.getItems().stream()
                    .filter(item -> item.getTipoTicket().getIdTipoTicket().equals(tipoTicket.getIdTipoTicket()) &&
                            item.getPrecio().equals(precioActual))
                    .findFirst()
                    .orElse(null);

            if (itemExistente != null) {
                log.info("Item ID {} ya existe. Añadiendo {} tickets.",
                        itemExistente.getIdItemCarrito(), itemReq.getCantidad());

                int nuevaCantidad = itemExistente.getCantidad() + itemReq.getCantidad();
                validarLimitePorPersona(tipoTicket, nuevaCantidad, cliente); // Validar total

                itemExistente.setCantidad(nuevaCantidad);
                itemExistente.calcularPrecioFinal();

                for (Ticket ticket : ticketsAReservar) {
                    ticket.setEstado(EstadoTicket.RESERVADA);
                    ticket.setCliente(cliente);
                    ticket.setItemCarrito(itemExistente);
                }
                itemExistente.getTickets().addAll(ticketsAReservar);
                itemCarritoRepository.save(itemExistente);

            } else {
                log.info("Creando nuevo ItemCarrito para TipoTicket ID {} '{}'",
                        tipoTicket.getIdTipoTicket(), tipoTicket.getNombre());

                ItemCarrito nuevoItem = new ItemCarrito();
                nuevoItem.setTipoTicket(tipoTicket);
                nuevoItem.setCantidad(itemReq.getCantidad());
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
            ticketRepository.saveAll(ticketsAReservar);
            tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() - itemReq.getCantidad());
            tipoTicketRepositorio.save(tipoTicket);

            log.info("✓ Procesado exitosamente: {} ticket(s) de '{}'",
                    itemReq.getCantidad(), tipoTicket.getNombre());
        }
        carro.recalcularTotales();
        carro.setFechaActualizacion(LocalDateTime.now().plusMinutes(TIEMPO_RESERVA_MINUTOS));
        CarroCompras carroGuardado = carroComprasRepository.save(carro);

        log.info("✓ Carrito ID {} actualizado exitosamente con {} item(s) total(es)",
                carroGuardado.getIdCarro(), carroGuardado.getItems().size());

        return convertirADTO(carroGuardado);
    }

    private void validarDisponibilidadTipoTicket(TipoTicket tipoTicket) {
        LocalDate hoy = LocalDate.now();

        if (tipoTicket.getFechaInicioVenta() != null && hoy.isBefore(tipoTicket.getFechaInicioVenta())) {
            throw new BusinessException("La venta para el tipo de ticket '" + tipoTicket.getNombre() +
                    "' aún no ha comenzado. Fecha de inicio: " + tipoTicket.getFechaInicioVenta());
        }
        if (tipoTicket.getFechaFinVenta() != null && hoy.isAfter(tipoTicket.getFechaFinVenta())) {
            throw new BusinessException("La venta para el tipo de ticket '" + tipoTicket.getNombre() +
                    "' ha finalizado. Fecha de fin: " + tipoTicket.getFechaFinVenta());
        }
        if (Boolean.FALSE.equals(tipoTicket.getActivo())) {
            throw new BusinessException("El tipo de ticket '" + tipoTicket.getNombre() +
                    "' no está disponible para la venta.");
        }
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
        ticket.setQrImageUrl(null);
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

    /**
     * Valida que el cliente tenga la edad mínima requerida para eventos +18.
     * Si el evento no permite menores de edad (menoresDeEdadPermitidos == false),
     * el cliente debe tener al menos 18 años.
     * 
     * @param evento El evento para el cual se está validando
     * @param cliente El cliente que intenta comprar
     * @throws BusinessException Si el cliente no cumple con la edad mínima requerida
     */
    private void validarEdadClienteParaEvento(pe.edu.pucp.fasticket.model.eventos.Evento evento, Cliente cliente) {
        if (evento == null) {
            log.warn("Evento es null, no se puede validar edad");
            return;
        }

        // Si el evento permite menores de edad, no hay restricción
        if (Boolean.TRUE.equals(evento.getMenoresDeEdadPermitidos())) {
            return;
        }

        // Si menoresDeEdadPermitidos es false, el evento es +18
        if (Boolean.FALSE.equals(evento.getMenoresDeEdadPermitidos())) {
            if (cliente.getFechaNacimiento() == null) {
                throw new BusinessException("No se puede verificar la edad. Por favor, actualiza tu fecha de nacimiento en tu perfil.");
            }

            LocalDate fechaNacimiento = cliente.getFechaNacimiento();
            LocalDate fechaActual = LocalDate.now();
            
            // Calcular edad comparando fecha de nacimiento con fecha actual
            int edad = fechaActual.getYear() - fechaNacimiento.getYear();
            
            // Ajustar si aún no ha cumplido años este año
            if (fechaActual.getMonthValue() < fechaNacimiento.getMonthValue() ||
                (fechaActual.getMonthValue() == fechaNacimiento.getMonthValue() &&
                 fechaActual.getDayOfMonth() < fechaNacimiento.getDayOfMonth())) {
                edad--;
            }

            if (edad < 18) {
                throw new BusinessException("Este evento es solo para mayores de 18 años. Tu edad actual es " + edad + " años.");
            }

            log.debug("Validación de edad exitosa: Cliente ID {} tiene {} años para evento +18 ID {}", 
                    cliente.getIdPersona(), edad, evento.getIdEvento());
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

        // Convertimos la lista de items y calculamos datos al vuelo
        List<ItemCarritoDTO> itemsDTO = carro.getItems().stream().map(item -> {
            ItemCarritoDTO itemDTO = new ItemCarritoDTO();
            itemDTO.setIdItemCarrito(item.getIdItemCarrito());
            itemDTO.setCantidad(item.getCantidad());

            if (item.getTipoTicket() != null) {
                TipoTicket tipo = item.getTipoTicket();

                // 1. Datos Básicos
                itemDTO.setIdTipoTicket(tipo.getIdTipoTicket());
                itemDTO.setNombreTicket(tipo.getNombre());

                // 2. Precio BASE (Original de BD)
                itemDTO.setPrecioBase(tipo.getPrecio());

                // 3. Calcular Descuentos/Etiquetas según la fecha de HOY
                TipoTicket.DetallePrecio detalle = tipo.getDetallePrecioActual();

                itemDTO.setEtiquetaPrecio(detalle.getEtiqueta()); // "PREVENTA"
                itemDTO.setTipoAjuste(detalle.getTipoAjuste());   // "DESCUENTO"

                // Calcular porcentaje visual: Si factor es 0.8 -> |1 - 0.8| = 0.2 -> 20%
                double pct = Math.abs(1.0 - detalle.getFactor()) * 100.0;
                itemDTO.setPorcentaje(Math.round(pct * 100.0) / 100.0);

                // 4. Precio Final Unitario
                double precioFinalCalculado = tipo.getPrecio() * detalle.getFactor();
                itemDTO.setPrecioUnitario(precioFinalCalculado);

                // 5. Subtotal de la línea
                itemDTO.setSubtotal(precioFinalCalculado * item.getCantidad());
            }
            return itemDTO;
        }).collect(Collectors.toList());

        dto.setItems(itemsDTO);

        // Recalcular Totales del Carrito basados en los items procesados
        // (Es más seguro recalcular aquí para que coincida con lo que mostramos)
        double sumaTotal = itemsDTO.stream()
                .mapToDouble(ItemCarritoDTO::getSubtotal)
                .sum();

        dto.setSubtotal(sumaTotal);
        dto.setTotal(sumaTotal); // (Aquí agregarías descuentos globales si tuvieras)

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

    @Override
    @Transactional
    public CarroComprasDTO eliminarItemDelCarrito(Integer idItemCarrito, Integer idCliente) {
        ItemCarrito item = itemCarritoRepository.findById(idItemCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("El item con ID " + idItemCarrito + " no existe."));

        if (!item.getCarroCompra().getCliente().getIdPersona().equals(idCliente)) {
            throw new SecurityException("Acción no permitida.");
        }

        CarroCompras carro = item.getCarroCompra();
        TipoTicket tipoTicket = item.getTipoTicket();
        int cantidadLiberada = 0;

        for (Ticket ticket : item.getTickets()) {
            if (ticket.getEstado() == EstadoTicket.RESERVADA) {
                ticket.setEstado(EstadoTicket.DISPONIBLE);
                ticket.setItemCarrito(null);
                ticket.setCliente(null);
                ticket.setNombreAsistente(null);
                ticket.setApellidoAsistente(null);
                ticket.setTipoDocumentoAsistente(null);
                ticket.setDocumentoAsistente(null);
                cantidadLiberada++;
            }
        }
        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() + cantidadLiberada);
        log.info("Liberados {} tickets del tipo {}", cantidadLiberada, tipoTicket.getNombre());

        carro.removeItem(item); // Elimina del carrito
        carro.setFechaActualizacion(LocalDateTime.now());

        // Si el carrito queda vacío, marcarlo como inactivo
        if (carro.getItems().isEmpty()) {
            carro.setActivo(false);
            log.info("Carrito ID {} marcado como inactivo (sin items)", carro.getIdCarro());
        }

        CarroCompras carroGuardado = carroComprasRepository.save(carro);
        return convertirADTO(carroGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResumenDTO> obtenerEventosDelCarrito(Integer idCarrito) {
        CarroCompras carrito = carroComprasRepository.findById(idCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (carrito.getItems().isEmpty()) {
            return new ArrayList<>();
        }
        return carrito.getItems().stream()
                .map(item -> item.getTipoTicket().getEvento())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(evento -> new EventoResumenDTO(
                        evento.getNombre(),
                        evento.getFechaEvento(),
                        evento.getHoraInicio(),
                        evento.getLocal() != null ? evento.getLocal().getNombre() : "Lugar por confirmar",
                        evento.getImagenUrl()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CarroComprasDTO incrementarCantidadItem(Integer idCliente, Integer idItemCarrito) {
        log.info("Incrementando cantidad del item ID: {} para cliente ID: {}", idItemCarrito, idCliente);
        ItemCarrito item = itemCarritoRepository.findById(idItemCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Item de carrito no encontrado: " + idItemCarrito));
        CarroCompras carro = item.getCarroCompra();
        if (!carro.getCliente().getIdPersona().equals(idCliente) || !carro.getActivo()) {
            throw new ResourceNotFoundException("El item no pertenece a un carrito activo del usuario actual");
        }
        TipoTicket tipoTicket = item.getTipoTicket();
        Cliente cliente = carro.getCliente();
        Double precioActual = tipoTicket.getPrecioCalculado();
        if (!item.getPrecio().equals(precioActual)) {
            throw new BusinessException("El precio del ticket ha cambiado. Por favor, elimina el item y agrégalo nuevamente.");
        }

        validarLimitePorPersona(tipoTicket, item.getCantidad() + 1, cliente);

        List<Ticket> ticketsDisponibles = ticketRepository.findAvailableTicketsByTypeAndState(
                tipoTicket, EstadoTicket.DISPONIBLE, PageRequest.of(0, 1)
        );

        if (ticketsDisponibles.isEmpty()) {
            throw new BusinessException("No hay stock disponible para " + tipoTicket.getNombre());
        }

        Ticket ticketAReservar = ticketsDisponibles.get(0);
        ticketAReservar.setEstado(EstadoTicket.RESERVADA);
        ticketAReservar.setCliente(cliente);
        ticketAReservar.setItemCarrito(item);

        ticketRepository.save(ticketAReservar);

        item.setCantidad(item.getCantidad() + 1);
        item.getTickets().add(ticketAReservar);
        item.calcularPrecioFinal();
        itemCarritoRepository.save(item);

        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() - 1);
        tipoTicketRepositorio.save(tipoTicket);

        log.info("Reservado 1 ticket adicional del tipo {} para item ID {}", tipoTicket.getNombre(), item.getIdItemCarrito());

        carro.recalcularTotales();
        carro.setFechaActualizacion(LocalDateTime.now().plusMinutes(TIEMPO_RESERVA_MINUTOS));
        CarroCompras carroGuardado = carroComprasRepository.save(carro);

        return convertirADTO(carroGuardado);
    }

    @Override
    @Transactional
    public CarroComprasDTO decrementarCantidadItem(Integer idCliente, Integer idItemCarrito) {
        log.info("Decrementando cantidad del item ID: {} para cliente ID: {}", idItemCarrito, idCliente);

        ItemCarrito item = itemCarritoRepository.findById(idItemCarrito)
                .orElseThrow(() -> new ResourceNotFoundException("Item de carrito no encontrado: " + idItemCarrito));
        CarroCompras carro = item.getCarroCompra();
        if (!carro.getCliente().getIdPersona().equals(idCliente) || !carro.getActivo()) {
            throw new ResourceNotFoundException("El item no pertenece a un carrito activo del usuario actual");
        }
        TipoTicket tipoTicket = item.getTipoTicket();

        if (item.getCantidad() <= 0) {
            throw new BusinessException("No hay tickets en este item para eliminar");
        }
        Ticket ticketALiberar = item.getTickets().stream()
                .filter(t -> t.getEstado() == EstadoTicket.RESERVADA)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException(
                        "Error de consistencia: No se encontró un ticket RESERVADO para liberar en este item"));

        ticketALiberar.setEstado(EstadoTicket.DISPONIBLE);
        ticketALiberar.setCliente(null);
        ticketALiberar.setItemCarrito(null);
        ticketALiberar.setNombreAsistente(null);
        ticketALiberar.setApellidoAsistente(null);
        ticketALiberar.setTipoDocumentoAsistente(null);
        ticketALiberar.setDocumentoAsistente(null);
        ticketALiberar.setCodigoQr(null);
        ticketALiberar.setQrImageUrl(null);

        ticketRepository.save(ticketALiberar);

        tipoTicket.setCantidadDisponible(tipoTicket.getCantidadDisponible() + 1);
        tipoTicketRepositorio.save(tipoTicket);

        log.info("Liberado 1 ticket del tipo {}", tipoTicket.getNombre());

        if (item.getCantidad() > 1) {
            item.setCantidad(item.getCantidad() - 1);
            item.getTickets().remove(ticketALiberar);
            item.calcularPrecioFinal();
            itemCarritoRepository.save(item);
            log.info("Item ID {} actualizado. Nueva cantidad: {}", item.getIdItemCarrito(), item.getCantidad());
        } else {
            carro.removeItem(item);
            log.info("Item ID {} eliminado del carrito (cantidad llegó a 0)", item.getIdItemCarrito());
        }
        carro.recalcularTotales();
        carro.setFechaActualizacion(LocalDateTime.now());
        if (carro.getItems().isEmpty()) {
            carro.setActivo(false);
            log.info("Carrito ID {} marcado como inactivo (sin items)", carro.getIdCarro());
        }
        CarroCompras carroGuardado = carroComprasRepository.save(carro);

        return convertirADTO(carroGuardado);
    }
}


