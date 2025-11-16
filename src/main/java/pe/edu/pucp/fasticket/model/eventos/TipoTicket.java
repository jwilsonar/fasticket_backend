package pe.edu.pucp.fasticket.model.eventos;

import java.time.LocalDate;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.exception.BusinessException;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"zona", "tickets"})
@ToString(exclude = {"zona", "tickets"})
@Entity
@Table(name = "TipoTicket")
public class TipoTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoTicket")
    private Integer idTipoTicket;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private Double precio;

    // genera para el stock inicial
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "cantidadDisponible", nullable = false)
    private Integer cantidadDisponible;

    @Column(name = "cantidadVendida")
    private Integer cantidadVendida = 0;

    @Column(name = "fechaInicioVenta")
    private LocalDateTime fechaInicioVenta;

    @Column(name = "fechaFinVenta")
    private LocalDateTime fechaFinVenta;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "limitePorPersona")
    private Integer limitePorPersona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idZona")
    private Zona zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEvento")
    private Evento evento;

    @OneToMany(mappedBy = "tipoTicket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "tipoTicket", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<PrecioEscalonado> preciosEscalonados = new ArrayList<>();

    public Double getPrecioCalculado() {
        if (this.evento == null || this.evento.getFechaEvento() == null) {
            return this.precio;
        }

        PrecioEscalonado precioCalculado = obtenerEscalonActual();

        LocalDate hoy = LocalDate.now();

        // 1. PREVENTA
        if (precioCalculado.getNombreEtapa() == Etapa.PREVENTA) {
            return this.precio * 0.80; // 20% Descuento
        }
        // 2. EARLY BIRD (Entre 7 y 14 días antes)
        if (precioCalculado.getNombreEtapa() == Etapa.EARLY_BIRD) {
            return this.precio * 0.90; // 10% Descuento
        }
        // 3. REGULAR (Entre 3 y 7 días antes)
        if (precioCalculado.getNombreEtapa() == Etapa.REGULAR) {
            return this.precio * 1.0; // 1.0 (Precio Base)
        }
        // 4. LATE (3 días antes o menos, hasta el día del evento)
        if (precioCalculado.getNombreEtapa() == Etapa.LATE) {
            return this.precio * 1.0; // 1.0 (Precio Base)
        }
        // Si no es ninguna de las anteriores, la compra es antes después del evento, no se vende
        throw new BusinessException("El evento ya pasó o aún no se habilita. No se pueden comprar tickets.");
    }

    public PrecioEscalonado obtenerEscalonActual() {
        LocalDate hoy = LocalDate.now();

        for (PrecioEscalonado esc : this.preciosEscalonados) {
            boolean estaDentroDeRango =
                    (hoy.isEqual(esc.getFechaInicio()) || hoy.isAfter(esc.getFechaInicio())) &&
                            (hoy.isEqual(esc.getFechaFin()) || hoy.isBefore(esc.getFechaFin()));

            if (estaDentroDeRango) {
                return esc;
            }
        }

        return null; // Si no hay ninguno
    }
}
