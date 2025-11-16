package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "DTO para asociar asistentes a un item del carrito")
public class AsistenteParaItemDTO {

    @Schema(description = "ID del item del carrito", example = "1", required = true)
    @NotNull(message = "El idItemCarrito es obligatorio")
    private Integer idItemCarrito;

    @Schema(description = "Lista de datos de los asistentes para este item", required = true)
    @NotEmpty(message = "La lista de asistentes no puede estar vacía")
    @Valid
    private List<DatosAsistenteDTO> asistentes;
}