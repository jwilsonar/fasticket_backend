package pe.edu.pucp.fasticket.model.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "Administrador")
@PrimaryKeyJoinColumn(name = "idPersona")
public class Administrador extends Persona {

    @Column(name = "cargo", length = 100)
    private String cargo;

    public Administrador() {
        super();
        this.setRol(Rol.ADMINISTRADOR);
    }
}
