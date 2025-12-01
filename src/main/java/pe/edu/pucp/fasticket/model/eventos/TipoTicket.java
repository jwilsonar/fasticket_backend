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
    private LocalDate fechaInicioVenta;

    @Column(name = "fechaFinVenta")
    private LocalDate fechaFinVenta;

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

        LocalDate hoy = LocalDate.now();

        // Validar que el evento no haya pasado
        if (hoy.isAfter(this.evento.getFechaEvento())) {
            throw new BusinessException("El evento ya pasó. No se pueden comprar tickets.");
        }

        // Buscar si hoy estamos dentro de una etapa especial
        if (preciosEscalonados != null) {
            for (PrecioEscalonado etapa : preciosEscalonados) {
                // [CORRECCIÓN] Comparamos LocalDate con LocalDate
                if (Boolean.TRUE.equals(etapa.getActivo()) &&
                        (hoy.isEqual(etapa.getFechaInicio()) || hoy.isAfter(etapa.getFechaInicio())) &&
                        (hoy.isEqual(etapa.getFechaFin()) || hoy.isBefore(etapa.getFechaFin()))) {

                    return etapa.getPrecio();
                }
            }
        }

        return this.precio;
    }

    public DetallePrecio getDetallePrecioActual() {
        if (this.evento == null || this.evento.getFechaEvento() == null) {
            return new DetallePrecio(1.0, "REGULAR", "NINGUNO");
        }
        if (LocalDate.now().isAfter(this.evento.getFechaEvento())) {
            return new DetallePrecio(1.0, "EVENTO FINALIZADO", "NINGUNO");
        }

        Double precioActual = getPrecioCalculado();

        if (!precioActual.equals(this.precio)) {
            String nombreEtapa = "OFERTA";
            if (preciosEscalonados != null) {
                nombreEtapa = preciosEscalonados.stream()
                        .filter(p -> p.getPrecio().equals(precioActual))
                        .findFirst()
                        .map(p -> p.getNombreEtapa().name())
                        .orElse("PROMOCIÓN");
            }

            double factor = precioActual / this.precio;
            String tipoAjuste = factor < 1.0 ? "DESCUENTO" : "SOBRECARGO";

            return new DetallePrecio(factor, nombreEtapa, tipoAjuste);
        }

        return new DetallePrecio(1.0, "REGULAR", "NINGUNO");
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DetallePrecio {
        private Double factor;
        private String etiqueta;
        private String tipoAjuste;
    }
}
