package pe.edu.pucp.fasticket.model.eventos;

import java.time.LocalDateTime;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.fidelizacion.Promocion;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"evento", "zona", "tickets", "promociones"})
@ToString(exclude = {"evento", "zona", "tickets", "promociones"})
@Entity
@Table(name = "categoria_entrada")
public class CategoriaEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria_entrada")
    private Integer idCategoriaEntrada;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    @Column(name = "cantidad_vendida")
    private Integer cantidadVendida = 0;

    @Column(name = "fecha_inicio_venta")
    private LocalDateTime fechaInicioVenta;

    @Column(name = "fecha_fin_venta")
    private LocalDateTime fechaFinVenta;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuario_creacion")
    private Integer usuarioCreacion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_actualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_zona", nullable = false)
    private Zona zona;

    @OneToMany(mappedBy = "categoriaEntrada", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "categoria_promocion",
        joinColumns = @JoinColumn(name = "id_categoria_entrada"),
        inverseJoinColumns = @JoinColumn(name = "id_promocion")
    )
    private List<Promocion> promocionesAplicables = new ArrayList<>();
}
