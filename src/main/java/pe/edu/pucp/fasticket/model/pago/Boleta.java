package pe.edu.pucp.fasticket.model.pago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "comprobantePago")
@ToString(exclude = "comprobantePago")
@Entity
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idBoleta")
    private Integer idBoleta;

    @Column(name = "dni", length = 255)
    private String dni;

    @Column(name = "nombreCliente", length = 255)
    private String nombreCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idComprobante")
    private ComprobantePago comprobantePago;
}
