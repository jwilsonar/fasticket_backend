package pe.edu.pucp.fasticket.model.fidelizacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "tiposTicket")
@ToString(exclude = "tiposTicket")
@Entity
@Table(name = "promocion")
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocion")
    private Integer idPromocion;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "codigo_descuento", unique = true, length = 50)
    private String codigoDescuento;

    @Column(name = "porcentaje_descuento")
    private Double porcentajeDescuento;

    @Column(name = "monto_fijo_descuento")
    private Double montoFijoDescuento;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToMany(mappedBy = "promocionesAplicables", fetch = FetchType.LAZY)
    private List<TipoTicket> tiposTicket = new ArrayList<>();
}
