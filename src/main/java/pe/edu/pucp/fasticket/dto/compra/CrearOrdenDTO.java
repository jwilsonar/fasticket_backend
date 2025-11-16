package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
    description = "DTO para crear una nueva orden de compra. " +
                  "IMPORTANTE: El cliente se obtiene automáticamente del token JWT de autenticación, " +
                  "no es necesario enviarlo en el cuerpo de la petición. " +
                  "Los datos de asistentes no se requieren en la creación de la orden."
)
public class CrearOrdenDTO {

    @Schema(
        description = "Identificador del cliente. Opcional en API (se toma del token), requerido en tests/unit.",
        example = "1",
        required = false
    )
    private Integer idCliente;
    
    @Schema(
        description = "Lista de items seleccionados para la compra. Cada item contiene el tipo de ticket y la cantidad deseada.",
        required = true,
        example = "[{\"idTipoTicket\": 1, \"cantidad\": 2}, {\"idTipoTicket\": 3, \"cantidad\": 1}]"
    )
    @NotEmpty(message = "La lista de items no puede estar vacía")
    @Valid
    private List<ItemSeleccionadoDTO> items;
}
