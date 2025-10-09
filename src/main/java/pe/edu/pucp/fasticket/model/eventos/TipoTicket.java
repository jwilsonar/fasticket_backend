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

}
