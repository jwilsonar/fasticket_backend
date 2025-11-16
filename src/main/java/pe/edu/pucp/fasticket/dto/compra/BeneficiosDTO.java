package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO para datos de beneficios al procesar una compra")
public class BeneficiosDTO {
    @Schema(description = "Indica si la orden de compra es canjeable por puntos", example = "true")
    private Boolean esCanjeable;
    @Schema(description = "Lista de códigos promocionales aplicados en la orden de compra en caso no sea canjeable")
    private List<Integer> idCodigosPromocionales;
    
    public BeneficiosDTO(Boolean esCanjeable, List<Integer> idCodigosPromocionales) {
        this.esCanjeable = esCanjeable;
        this.idCodigosPromocionales = idCodigosPromocionales;
    }
}
