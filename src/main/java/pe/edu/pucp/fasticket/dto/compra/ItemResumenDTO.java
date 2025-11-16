package pe.edu.pucp.fasticket.dto.compra;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO de resumen de un item de orden")
public class ItemResumenDTO {
    
    @Schema(description = "Cantidad de tickets del tipo", example = "2")
    private int cantidad;
    
    @Schema(description = "Nombre del tipo de ticket", example = "VIP")
    private String nombreTipoTicket;
    
    @Schema(description = "Precio unitario del ticket", example = "50.00")
    private double precioUnitario;
}
