package pe.edu.pucp.fasticket.dto.compra;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class CrearOrdenDTO {
    private Integer idCliente;
    private List<ItemSeleccionadoDTO> items;
}
