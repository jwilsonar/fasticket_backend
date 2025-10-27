package pe.edu.pucp.fasticket.model.pago;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "comprobantePago")
@ToString(exclude = "comprobantePago")
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago")
    private Integer idPago;

    @Column(name = "metodo", length = 255)
    private String metodo;

    @Column(name = "monto")
    private Double monto;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

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

    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ComprobantePago comprobantePago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOrden")
    private OrdenCompra ordenCompra;
}
