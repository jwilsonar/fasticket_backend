package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO Response para datos al procesar una compra")
public class ProcesarCompraResponseDTO {    

    @Schema(description = "Lista de ítems con sus cantidades en la orden de compra")
    private List<ItemsDTO> itemsDTOList;
    @Schema(description = "Beneficios aplicados a la orden de compra")
    private BeneficiosDTO beneficios;

    public ProcesarCompraResponseDTO(List<ItemsDTO> itemsDTOList, BeneficiosDTO beneficios) {
        this.itemsDTOList = itemsDTOList;
        this.beneficios = beneficios;
    }
}
