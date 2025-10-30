package pe.edu.pucp.fasticket.model.fidelizacion;

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
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"codigoPromocional", "ordenCompra"})
@ToString(exclude = {"codigoPromocional", "ordenCompra"})
@Entity
@Table(name = "DescuentosRealizados")
public class DescuentosRealizados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDescuentoRealizado")
    private Integer idDescuentoRealizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCodigoPromocional", nullable = false)
    private CodigoPromocional codigoPromocional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOrdenCompra", nullable = false)
    private OrdenCompra ordenCompra;

    @Column(name = "valor")
    private Double valor;
}
