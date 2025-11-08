package pe.edu.pucp.fasticket.dto.geografia;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

@Data
@NoArgsConstructor
public class DistritoDTO {
    private Integer idDistrito;
    private String nombre;

    public DistritoDTO(Distrito distrito) {
        this.idDistrito = distrito.getIdDistrito();
        this.nombre = distrito.getNombre();
    }
}