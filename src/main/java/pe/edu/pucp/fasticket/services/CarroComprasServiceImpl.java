package pe.edu.pucp.fasticket.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CarroComprasServiceImpl implements CarroComprasService {

    private final CarroComprasRepository carroComprasRepository;
    private final ClienteRepository clienteRepository;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final ItemCarritoRepository itemCarritoRepository;
    private final TicketRepository ticketRepository;

    private static final int LIMITE_MAXIMO_TICKETS_POR_CLIENTE = 10;
    private static final int TIEMPO_RESERVA_MINUTOS = 15;

    @Override
    @Transactional
    public CarroComprasDTO agregarItemAlCarrito(AddItemRequestDTO request) {
        log.info("Agregando item (simple) al carrito para cliente ID: {}", request.getIdCliente());

        validarItemYAsistentes(request);

        TipoTicket tipoTicket = tipoTicketRepositorio.findById(request.getIdTipoTicket())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado: " + request.getIdTipoTicket()));

        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.getIdCliente()));

        validarLimitePorPersona(tipoTicket, request.getCantidad(), cliente);

        if (tipoTicket.getCantidadDisponible() < request.getCantidad()) {
            throw new BusinessException("Stock insuficiente para: " + tipoTicket.getNombre());
        }

        CarroCompras carro = carroComprasRepository.findByCliente_IdPersona(cliente.getIdPersona())
                .orElseGet(() -> {
                    log.info("Creando nuevo carrito para cliente ID: {}", cliente.getIdPersona());
                    CarroCompras nuevoCarro = new CarroCompras();
                    nuevoCarro.setCliente(cliente);
                    nuevoCarro.setFechaCreacion(LocalDateTime.now());
                    nuevoCarro.setIdEventoActual(tipoTicket.getEvento().getIdEvento());
                    return nuevoCarro;
                });

        carro.setActivo(true);

        if (carro.getIdEventoActual() != null) {
            if (!carro.getIdEventoActual().equals(tipoTicket.getEvento().getIdEvento())) {
                throw new BusinessException("No puedes añadir tickets de diferentes eventos al mismo carrito.");
            }
        } else {
            carro.setIdEventoActual(tipoTicket.getEvento().getIdEvento());
        }

        ItemCarrito itemExistente = null;
        for (ItemCarrito item : carro.getItems()) {
            if (item.getTipoTicket().getIdTipoTicket().equals(tipoTicket.getIdTipoTicket())) {
                itemExistente = item;
                break;
            }
        }

        if (itemExistente != null) {
            int nuevaCantidad = itemExistente.getCantidad() + request.getCantidad();
            validarLimitePorPersona(tipoTicket, nuevaCantidad, cliente);

            if (tipoTicket.getCantidadDisponible() < nuevaCantidad) {
                throw new BusinessException("Stock insuficiente (sumando lo que ya tenías en el carro).");
            }
            itemExistente.setCantidad(nuevaCantidad);
            itemExistente.calcularPrecioFinal();
        } else {
            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setTipoTicket(tipoTicket);
            nuevoItem.setCantidad(request.getCantidad());
            nuevoItem.setPrecio(tipoTicket.getPrecio());
            nuevoItem.setFechaAgregado(LocalDate.now());
            nuevoItem.setCarroCompra(carro);
            nuevoItem.calcularPrecioFinal();
            carro.addItem(nuevoItem);
        }

        carro.recalcularTotales();
        carro.setFechaActualizacion(LocalDateTime.now());
        CarroCompras carroGuardado = carroComprasRepository.save(carro);
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
        CarroCompras carroGuardado = carroComprasRepository.save(carro);
        return convertirADTO(carroGuardado);
    }

    private void validarItemYAsistentes(AddItemRequestDTO item) {
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

    private String generarCodigoQrUnico() {
        return java.util.UUID.randomUUID().toString();
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

    private byte[] generarQrComoBytes(String contenido) {
        try {
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            var matrix = writer.encode(contenido, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
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

    @Override
    @Transactional(readOnly = true)
    public CarroComprasDTO verCarrito(Integer idCliente) {
        return carroComprasRepository.findByCliente_IdPersona(idCliente)
                .map(this::convertirADTO) // Si encuentra el carrito, lo convierte a DTO
                .orElseGet(() -> crearCarritoVacioDTO()); // Si no, devuelve un DTO de un carrito vacío
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
                itemDTO.setPrecioUnitario(item.getTipoTicket().getPrecio());
                itemDTO.setSubtotal(item.getTipoTicket().getPrecio() * item.getCantidad());
            }
            return itemDTO;
        }).collect(Collectors.toList()));

        return dto;
    }

    private CarroComprasDTO crearCarritoVacioDTO() {
        CarroComprasDTO dto = new CarroComprasDTO();
        dto.setIdCarro(null);
        dto.setSubtotal(0.0);
        dto.setTotal(0.0);
        dto.setItems(Collections.emptyList());
        return dto;
    }
}