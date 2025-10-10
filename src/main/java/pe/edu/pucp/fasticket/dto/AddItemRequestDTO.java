package pe.edu.pucp.fasticket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddItemRequestDTO {
    private Integer idTipoTicket;
    private Integer cantidad;
    private Integer idCliente;
}