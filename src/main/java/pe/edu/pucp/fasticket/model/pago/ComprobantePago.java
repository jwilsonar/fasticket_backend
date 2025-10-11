package pe.edu.pucp.fasticket.model.pago;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"pago", "boletas"})
@ToString(exclude = {"pago", "boletas"})
@Entity
@Table(name = "comprobantepago")
public class ComprobantePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idComprobante")
    private Integer idComprobante;

    @Column(name = "numero_serie", length = 255)
    private String numeroSerie;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "total")
    private Double total;

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

    @Column(name = "dni", length = 255)
    private String dni;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPago", unique = true)
    private Pago pago;

    @OneToMany(mappedBy = "comprobantePago", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Boleta> boletas = new ArrayList<>();
}
