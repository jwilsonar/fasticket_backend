package pe.edu.pucp.fasticket.model.compra;

import java.time.LocalDate;

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
import pe.edu.pucp.fasticket.model.eventos.Ticket;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"carroCompra", "ordenCompra", "ticket"})
@ToString(exclude = {"carroCompra", "ordenCompra", "ticket"})
@Entity
@Table(name = "item_carrito")
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_carrito")
    private Integer idItemCarrito;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "descuento")
    private Double descuento = 0.0;

    @Column(name = "precio_final")
    private Double precioFinal;

    @Column(name = "fecha_agregado")
    private LocalDate fechaAgregado;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carro")
    private CarroCompras carroCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden_compra")
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false)
    private Ticket ticket;

    public void calcularPrecioFinal() {
        this.precioFinal = (this.precio * this.cantidad) - this.descuento;
    }
}
