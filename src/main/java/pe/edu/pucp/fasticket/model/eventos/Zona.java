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
@Table(name = "zona")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCategoria") // Según el esquema SQL
    private Integer idCategoria;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "aforoMax")
    private Integer aforoMax;

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

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Local> locales = new ArrayList<>();

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CategoriaEntrada> categoriasEntrada = new ArrayList<>();
}
