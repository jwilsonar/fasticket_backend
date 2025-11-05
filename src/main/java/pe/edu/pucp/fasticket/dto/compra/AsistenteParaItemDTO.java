package pe.edu.pucp.fasticket.dto.compra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AsistenteParaItemDTO {

    @NotNull(message = "El idItemCarrito es obligatorio")
    private Integer idItemCarrito;

    @NotEmpty(message = "La lista de asistentes no puede estar vacía")
    @Valid
    private List<DatosAsistenteDTO> asistentes;
}