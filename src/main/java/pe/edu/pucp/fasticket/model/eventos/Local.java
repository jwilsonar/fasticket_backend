package pe.edu.pucp.fasticket.model.eventos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"distrito"})
@ToString(exclude = {"distrito"})
@Entity
@Table(name = "Local")
@Table(name = "Local")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLocal")
    @Column(name = "idLocal")
    private Integer idLocal;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "aforoTotal")
    @Column(name = "aforoTotal")
    private Integer aforoTotal;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuarioCreacion")
    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion")
    @Column(name = "fechaCreacion")
    private java.time.LocalDate fechaCreacion;

    @Column(name = "usuarioActualizacion")
    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion")
    @Column(name = "fechaActualizacion")
    private java.time.LocalDate fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idDistrito")
    private Distrito distrito;
}
