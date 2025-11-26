package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar un código promocional individual en el request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para un código promocional a aplicar")
public class CodigoPromocionalItemDTO {
    
    @Schema(description = "ID del código promocional a aplicar", example = "1", required = true)
    @NotNull(message = "El ID del código promocional es obligatorio")
    private Integer idCodigoPromocional;
}

