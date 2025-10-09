package pe.edu.pucp.fasticket.model.eventos;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"locales", "categoriasEntrada"})
@ToString(exclude = {"locales", "categoriasEntrada"})
@Entity
@Table(name = "Zona")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idZona")
    private Integer idZona;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "aforoMax")
    private Integer aforoMax;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion")
    private java.time.LocalDate fechaCreacion;

    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion")
    private java.time.LocalDate fechaActualizacion;


}
