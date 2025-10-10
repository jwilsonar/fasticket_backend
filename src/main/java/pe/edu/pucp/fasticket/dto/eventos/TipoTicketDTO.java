package pe.edu.pucp.fasticket.dto.eventos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TipoTicketDTO {
    private Integer id;
    private String nombre;
    private Double precio;
}
