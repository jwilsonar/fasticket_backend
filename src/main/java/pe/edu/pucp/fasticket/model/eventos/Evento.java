package pe.edu.pucp.fasticket.model.eventos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"local", "tickets", "categoriasEntrada"})
@ToString(exclude = {"local", "tickets", "categoriasEntrada"})
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer idEvento;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "aforo_disponible")
    private Integer aforoDisponible;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "tipo_evento")
    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    @Column(name = "estado_evento")
    @Enumerated(EnumType.STRING)
    private EstadoEvento estadoEvento;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuario_creacion")
    private Integer usuarioCreacion;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Column(name = "usuario_actualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fecha_actualizacion")
    private LocalDate fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idLocal", nullable = false)
    private Local local;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CategoriaEntrada> categoriasEntrada = new ArrayList<>();
}
