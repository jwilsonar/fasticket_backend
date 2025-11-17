package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO para información de item en historial")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemHistorialDTO {
    
    @Schema(description = "ID del item", example = "1")
    private Integer idItemCarrito;
    
    @Schema(description = "Cantidad de tickets", example = "2")
    private Integer cantidad;
    
    @Schema(description = "Precio unitario", example = "75.00")
    private Double precio;
    
    @Schema(description = "Precio final", example = "150.00")
    private Double precioFinal;
    
    @Schema(description = "Nombre del tipo de ticket", example = "VIP")
    private String tipoTicketNombre;
}

