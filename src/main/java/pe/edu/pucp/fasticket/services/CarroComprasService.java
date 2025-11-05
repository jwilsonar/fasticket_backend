package pe.edu.pucp.fasticket.services;

import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;

public interface CarroComprasService {

    CarroComprasDTO agregarItemAlCarrito(AddItemRequestDTO request);
    CarroComprasDTO verCarrito(Integer idCliente);
    public CarroComprasDTO eliminarTicketIndividualDelCarrito(Integer idTicket, Integer idCliente);
}