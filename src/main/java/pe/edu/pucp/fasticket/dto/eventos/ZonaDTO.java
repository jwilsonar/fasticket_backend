package pe.edu.pucp.fasticket.dto.eventos;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.Zona;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ZonaDTO {
    private Integer idZona, aforoMax, usuarioCreacion, usuarioActualizacion;
    private String nombre;
    private Boolean activo;
    private LocalDate fechaCreaion, fechaActualizacion;

    public ZonaDTO(Zona p_zona){
        this.idZona = p_zona.getIdZona();
        this.nombre = p_zona.getNombre();
        this.activo = p_zona.getActivo();
        this.fechaCreaion = p_zona.getFechaCreacion();
        this.fechaActualizacion = p_zona.getFechaActualizacion();
        this.usuarioCreacion = p_zona.getUsuarioCreacion();
        this.usuarioActualizacion = p_zona.getUsuarioActualizacion();
    }
}
