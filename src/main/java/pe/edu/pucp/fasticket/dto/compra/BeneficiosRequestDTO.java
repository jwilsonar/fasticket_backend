package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar beneficios en el checkout.
 * Los beneficios incluyen canje de puntos y códigos promocionales.
 * El canje de puntos y códigos promocionales son mutuamente excluyentes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para beneficios aplicables en el checkout (canje de puntos o códigos promocionales)")
public class BeneficiosRequestDTO {
    
    @Schema(
        description = "Indica si se desea canjear puntos por esta compra. " +
                      "Si es true, el cliente debe tener puntos suficientes para cubrir el total. " +
                      "Cuando es canjeable, no se pueden aplicar códigos promocionales.",
        example = "false",
        required = true
    )
    @NotNull(message = "El campo canjeable es obligatorio")
    private Boolean canjeable;
    
    @Schema(
        description = "Lista de códigos promocionales a aplicar. " +
                      "Solo se procesa si canjeable=false. " +
                      "Se pueden aplicar múltiples códigos promocionales que se suman.",
        required = false
    )
    @Valid
    private List<CodigoPromocionalItemDTO> codigosPromocionales;
}

