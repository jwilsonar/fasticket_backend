package pe.edu.pucp.fasticket.dto.geografia;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.geografia.Provincia;

@Data
@NoArgsConstructor
public class ProvinciaDTO {
    private Integer idProvincia;
    private String nombre;

    public ProvinciaDTO(Provincia provincia) {
        this.idProvincia = provincia.getIdProvincia();
        this.nombre = provincia.getNombre();
    }
}