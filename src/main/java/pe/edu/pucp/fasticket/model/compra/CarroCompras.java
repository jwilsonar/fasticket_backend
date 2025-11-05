package pe.edu.pucp.fasticket.model.compra;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"cliente", "items"})
@ToString(exclude = {"cliente", "items"})
@Entity
@Table(name = "carro_compras")
public class CarroCompras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carro")
    private Integer idCarro;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "subtotal")
    private Double subtotal = 0.0;

    @Column(name = "total")
    private Double total = 0.0;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    @OneToMany(mappedBy = "carroCompra", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ItemCarrito> items = new ArrayList<>();

    @OneToOne(mappedBy = "carroCompras", fetch = FetchType.LAZY)
    private OrdenCompra ordenCompra;

    @Column(name = "idEventoActual")
    private Integer idEventoActual;

    public void addItem(ItemCarrito item) {
        items.add(item);
        item.setCarroCompra(this);
        recalcularTotales();
    }

    public void removeItem(ItemCarrito item) {
        items.remove(item);
        item.setCarroCompra(null);
        recalcularTotales();
    }

    public void recalcularTotales() {
        this.subtotal = items.stream()
            .mapToDouble(item -> item.getPrecio() * item.getCantidad())
            .sum();
        this.total = this.subtotal;
    }
}
