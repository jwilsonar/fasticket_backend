package pe.edu.pucp.fasticket.services;

import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResumenDTO;

import java.util.List;

public interface CarroComprasService {

    CarroComprasDTO agregarItemAlCarrito(AddItemRequestDTO request);
    CarroComprasDTO verCarrito(Integer idCliente);
    public CarroComprasDTO eliminarTicketIndividualDelCarrito(Integer idTicket, Integer idCliente);
    public CarroComprasDTO aplicarCodigoPromocional(Integer idCarrito, String codigo);
    public CarroComprasDTO eliminarItemDelCarrito(Integer idItemCarrito, Integer idCliente);
    public List<EventoResumenDTO> obtenerEventosDelCarrito(Integer idCarrito);
}