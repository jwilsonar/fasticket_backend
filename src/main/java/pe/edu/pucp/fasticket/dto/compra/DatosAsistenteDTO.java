package pe.edu.pucp.fasticket.dto.compra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;

@Data
@NoArgsConstructor
public class DatosAsistenteDTO {
    @NotNull
    private TipoDocumento tipoDocumento;
    @NotBlank
    private String numeroDocumento;
    @NotBlank
    private String nombres;
    @NotBlank
    private String apellidos;

    public DatosAsistenteDTO(TipoDocumento tipoDocumento, String numeroDocumento, String nombres, String apellidos) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }
}