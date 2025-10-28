package pe.edu.pucp.fasticket.dto.eventos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoTicketCompraDTO {
    private Integer idTipoTicket;
    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer cantidadDisponible;
}
