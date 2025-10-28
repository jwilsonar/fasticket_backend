package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO para item seleccionado en una orden de compra")
public class ItemSeleccionadoDTO {
    
    @Schema(description = "ID del tipo de ticket seleccionado", example = "1", required = true)
    private Integer idTipoTicket;
    
    @Schema(description = "Cantidad de tickets a comprar", example = "2", required = true, minimum = "1")
    private int cantidad;
    
    @Schema(description = "Lista de datos de los asistentes para cada ticket", required = true)
    private List<DatosAsistenteDTO> asistentes;
}
