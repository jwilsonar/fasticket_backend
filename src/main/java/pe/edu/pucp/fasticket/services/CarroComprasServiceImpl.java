package pe.edu.pucp.fasticket.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.dto.ItemCarritoDTO;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.ClienteRepository;
import pe.edu.pucp.fasticket.repository.ItemCarritoRepository;
import pe.edu.pucp.fasticket.repository.TipoTicketRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarroComprasServiceImpl implements CarroComprasService {

    private final CarroComprasRepository carroComprasRepository;
    private final ClienteRepository clienteRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final ItemCarritoRepository itemCarritoRepository;

    @Override
    @Transactional
    public CarroComprasDTO agregarItemAlCarrito(AddItemRequestDTO request) {
        // 1. Validar que el tipo de ticket existe y hay stock
        TipoTicket tipoTicket = tipoTicketRepository.findById(request.getIdTipoTicket())
                .orElseThrow(() -> new RuntimeException("Tipo de ticket no encontrado con ID: " + request.getIdTipoTicket()));

        if (tipoTicket.getCantidadDisponible() < request.getCantidad()) {
            throw new RuntimeException("Stock insuficiente para el ticket: " + tipoTicket.getNombre());
        }

        // 2. Buscar al cliente
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getIdCliente()));

        // 3. Obtener el carrito del cliente o crear uno nuevo si no existe
        CarroCompras carro = carroComprasRepository.findByClienteIdPersona(cliente.getIdPersona())
                .orElseGet(() -> {
                    CarroCompras nuevoCarro = new CarroCompras();
                    nuevoCarro.setCliente(cliente);
                    nuevoCarro.setFechaCreacion(LocalDateTime.now());
                    return nuevoCarro;
                });

        // 4. REGLA DE NEGOCIO: Validar si el carrito está vacío o si el ticket es del mismo evento
        if (carro.getItems().isEmpty()) {
            // Si el carrito es nuevo, se le asigna el evento del primer item que se agrega
            carro.setIdEventoActual(tipoTicket.getEvento().getIdEvento());
        } else if (!carro.getIdEventoActual().equals(tipoTicket.getEvento().getIdEvento())) {
            // Si ya hay items, se valida que el nuevo item sea del mismo evento
            throw new RuntimeException("No puedes añadir tickets de diferentes eventos al mismo carrito. Vacía el carrito o finaliza tu compra actual.");
        }

        // 5. Crear el nuevo item y añadirlo al carrito
        ItemCarrito nuevoItem = new ItemCarrito();
        nuevoItem.setTipoTicket(tipoTicket);
        nuevoItem.setCantidad(request.getCantidad());
        nuevoItem.setPrecio(tipoTicket.getPrecio());
        nuevoItem.setFechaAgregado(java.time.LocalDate.now());

        carro.addItem(nuevoItem); // Usa el método del modelo para añadir y recalcular totales
        carro.setFechaActualizacion(LocalDateTime.now()); // Actualiza la fecha para la regla de expiración de 15 mins

        // 6. Guardar los cambios en la base de datos
        CarroCompras carroGuardado = carroComprasRepository.save(carro);

        // 7. Convertir la entidad a DTO para la respuesta
        return convertirADTO(carroGuardado);
    }

    @Override
    @Transactional
    public CarroComprasDTO eliminarItemDelCarrito(Integer idItemCarrito, Integer idCliente) {
        // 1. Buscamos el item a eliminar
        ItemCarrito item = itemCarritoRepository.findById(idItemCarrito)
                .orElseThrow(() -> new RuntimeException("El item con ID " + idItemCarrito + " no existe."));

        // 2. Validamos que el item pertenezca al carrito del cliente que hace la petición
        if (!item.getCarroCompra().getCliente().getIdPersona().equals(idCliente)) {
            throw new SecurityException("Acción no permitida. No puedes eliminar un item que no está en tu carrito.");
        }

        CarroCompras carro = item.getCarroCompra();
        carro.removeItem(item); // Usa el método del modelo para eliminar y recalcular

        // 3. REGLA DE NEGOCIO: Si el carrito queda vacío, reseteamos el evento
        if (carro.getItems().isEmpty()) {
            carro.setIdEventoActual(null);
        }

        carro.setFechaActualizacion(LocalDateTime.now());
        CarroCompras carroGuardado = carroComprasRepository.save(carro);

        return convertirADTO(carroGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public CarroComprasDTO verCarrito(Integer idCliente) {
        return carroComprasRepository.findByClienteIdPersona(idCliente)
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