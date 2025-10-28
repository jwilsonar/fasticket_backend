package pe.edu.pucp.fasticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;

import java.util.List;

@Data
@NoArgsConstructor
public class AddItemRequestDTO {
    private Integer idTipoTicket;
    private Integer cantidad;
    private Integer idCliente;
    @Valid
    @NotEmpty
    private List<DatosAsistenteDTO> asistentes;
}