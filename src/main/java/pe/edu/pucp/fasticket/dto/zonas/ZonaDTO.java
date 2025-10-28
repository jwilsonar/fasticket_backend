package pe.edu.pucp.fasticket.dto.zonas;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ZonaDTO {
    private Integer idZona, aforoMax, usuarioCreacion, usuarioActualizacion;
    private String nombre;
    private Boolean activo;
    private LocalDate fechaCreacion, fechaActualizacion;
    private Integer idLocal;
}
