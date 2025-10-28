package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO para crear una nueva orden de compra")
public class CrearOrdenDTO {
    
    @Schema(description = "ID del cliente que realiza la compra", example = "1", required = true)
    private Integer idCliente;
    
    @Schema(description = "Lista de items seleccionados para la compra", required = true)
    private List<ItemSeleccionadoDTO> items;
}
