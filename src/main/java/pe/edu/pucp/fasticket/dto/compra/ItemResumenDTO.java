package pe.edu.pucp.fasticket.dto.compra;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemResumenDTO {
    private int cantidad;
    private String nombreTipoTicket;
    private double precioUnitario;
}
