package pe.edu.pucp.fasticket.dto.compra;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ItemSeleccionadoDTO {
    private Integer idTipoTicket;
    private int cantidad;
    private List<DatosAsistenteDTO> asistentes;
}
