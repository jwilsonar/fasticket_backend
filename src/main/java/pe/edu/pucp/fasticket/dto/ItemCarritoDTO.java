package pe.edu.pucp.fasticket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemCarritoDTO {
    private Integer idItemCarrito;
    private Integer idTipoTicket;
    private String nombreTicket;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
}