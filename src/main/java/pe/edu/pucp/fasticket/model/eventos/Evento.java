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

    /**
     * RF-072: Edad mínima requerida para asistir al evento.
     * Por ejemplo: 18 para eventos con restricción de edad, 0 para todos los públicos.
     */
    @Column(name = "edadMinima")
    private Integer edadMinima = 0;

    /**
     * RF-072: Restricciones adicionales del evento (ej: "No se permite el ingreso de alimentos").
     */
    @Column(name = "restricciones", length = 1000)
    private String restricciones;

    /**
     * RF-073: Políticas de devolución o cambio específicas del evento.
     */
    @Column(name = "politicasDevolucion", length = 1000)
    private String politicasDevolucion;

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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idLocal")
    private Local local;
}
