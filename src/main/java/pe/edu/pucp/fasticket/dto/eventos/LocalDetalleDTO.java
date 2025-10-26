package pe.edu.pucp.fasticket.dto.eventos;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

@Data
@NoArgsConstructor
public class LocalDetalleDTO {
    private String nombre;
    private String direccion;
    private String urlMapa;
    private Distrito distrito;
}
