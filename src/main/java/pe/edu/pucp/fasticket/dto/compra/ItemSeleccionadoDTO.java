package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;

@Data
@NoArgsConstructor
@Schema(
    description = "DTO para item seleccionado en una orden de compra. " +
                  "Solo requiere el tipo de ticket y la cantidad. " +
                  "Los datos de asistentes no se solicitan en la creación de la orden."
)
public class ItemSeleccionadoDTO {
    
    @Schema(
        description = "ID del tipo de ticket que se desea comprar. Debe existir y estar disponible.",
        example = "1",
        required = true
    )
    @NotNull(message = "El ID del tipo de ticket es obligatorio")
    private Integer idTipoTicket;
    
    @Schema(
        description = "Cantidad de tickets del tipo especificado que se desean comprar. Debe ser al menos 1.",
        example = "2",
        required = true,
        minimum = "1"
    )
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @Schema(
        description = "Lista de asistentes correspondiente a los tickets de este item. Se usa en flujos de asignación posterior y en tests/unit.",
        required = false
    )
    @Valid
    private List<DatosAsistenteDTO> asistentes;
}
