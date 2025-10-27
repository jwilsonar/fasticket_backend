package pe.edu.pucp.fasticket.dto.usuario;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PersonaDTO {
    private Integer idPersona;
    private Integer usuario_creacion, usuario_actualizacion;
    private String docIdentidad, nombres, apellidos,  telefono, email, direccion, contrasena;
    private LocalDate fechaNacimiento, fecha_creacion, fecha_actualizacion;
    private Boolean activo;
    private TipoDocumento tipoDocumento;
    private Rol rol;

    public PersonaDTO(Persona p_persona) {
        this.idPersona = p_persona.getIdPersona();
        this.usuario_creacion = p_persona.getUsuarioCreacion();
        this.usuario_actualizacion = p_persona.getUsuarioActualizacion();

        this.docIdentidad = p_persona.getDocIdentidad();
        this.nombres = p_persona.getNombres();
        this.apellidos = p_persona.getApellidos();
        this.telefono = p_persona.getTelefono();
        this.email = p_persona.getEmail();
        this.direccion = p_persona.getDireccion();
        this.contrasena = p_persona.getContrasena();
        this.fechaNacimiento = p_persona.getFechaNacimiento();
        this.fecha_creacion = p_persona.getFechaCreacion();
        this.fecha_actualizacion = p_persona.getFechaActualizacion();
        this.activo = p_persona.getActivo();
        this.tipoDocumento = p_persona.getTipoDocumento();
        this.rol = p_persona.getRol();
    }
}
