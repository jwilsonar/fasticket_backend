package pe.edu.pucp.fasticket.dto.compra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutCarritoRequestDTO {

    @NotEmpty(message = "La lista de items no puede estar vacía")
    @Valid
    private List<AsistenteParaItemDTO> itemsConAsistentes;
}
