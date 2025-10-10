package pe.edu.pucp.fasticket.dto.eventos;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class LocalDTO {
    private Integer idLocal, aforoTotal, usuarioCreacion, usuarioActualizacion;
    private String nombre, direccion;
    private Distrito distrito;
    private Boolean activo;
    private LocalDate fechaActualizacion, fechaCreacion;

    public LocalDTO(Local p_local){
        this.idLocal = p_local.getIdLocal();
        this.aforoTotal = p_local.getAforoTotal();
        this.usuarioCreacion = p_local.getUsuarioCreacion();
        this.usuarioActualizacion = p_local.getUsuarioActualizacion();
        this.nombre = p_local.getNombre();
        this.direccion = p_local.getDireccion();
        this.distrito = p_local.getDistrito();
        this.activo = p_local.getActivo();
        this.fechaActualizacion = p_local.getFechaCreacion();
        this.fechaCreacion = p_local.getFechaCreacion();
        //Faltan algunos campos, pero voy a consultar, asi que NOTA
    }
}
