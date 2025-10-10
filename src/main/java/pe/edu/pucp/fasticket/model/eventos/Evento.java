package pe.edu.pucp.fasticket.model.eventos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"local", "tickets", "tiposTicket"})
@ToString(exclude = {"local", "tickets", "tiposTicket"})
@Entity
@Table(name = "Evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEvento")
    private Integer idEvento;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fechaEvento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "horaInicio")
    private LocalTime horaInicio;

    @Column(name = "horaFin")
    private LocalTime horaFin;

    @Column(name = "aforoDisponible")
    private Integer aforoDisponible;

    @Column(name = "imagenUrl", length = 500)
    private String imagenUrl;

    @Column(name = "tipoEvento")
    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    @Column(name = "estadoEvento")
    @Enumerated(EnumType.STRING)
    private EstadoEvento estadoEvento;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion")
    private LocalDate fechaCreacion;

    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion")
    private LocalDate fechaActualizacion;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TipoTicket> tiposTicket = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idLocal")
    private Local local;
}
