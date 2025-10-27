package pe.edu.pucp.fasticket.dto.compra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RegistrarParticipantesDTO {
    @NotEmpty
    @Valid
    private List<DatosAsistenteDTO> participantes;
}
