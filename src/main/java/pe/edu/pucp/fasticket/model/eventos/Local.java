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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"distrito", "eventos", "zona"})
@ToString(exclude = {"distrito", "eventos", "zona"})
@Entity
@Table(name = "local")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_local")
    private Integer idLocal;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "aforo_total")
    private Integer aforoTotal;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuario_creacion")
    private Integer usuarioCreacion;

    @Column(name = "fecha_creacion")
    private java.time.LocalDate fechaCreacion;

    @Column(name = "usuario_actualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fecha_actualizacion")
    private java.time.LocalDate fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idDistrito")
    private Distrito distrito;

    @OneToMany(mappedBy = "local", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Evento> eventos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCategoria")
    private Zona zona;
}
