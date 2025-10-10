package pe.edu.pucp.fasticket.services;

import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;

public interface CarroComprasService {

    CarroComprasDTO agregarItemAlCarrito(AddItemRequestDTO request);
    CarroComprasDTO verCarrito(Integer idCliente);
    CarroComprasDTO eliminarItemDelCarrito(Integer idItemCarrito, Integer idCliente);
}