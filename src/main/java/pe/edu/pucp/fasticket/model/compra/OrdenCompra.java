package pe.edu.pucp.fasticket.model.compra;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"cliente", "items", "carroCompras"})
@ToString(exclude = {"cliente", "items", "carroCompras"})
@Entity
@Table(name = "orden_compra")
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_compra")
    private Integer idOrdenCompra;

    @Column(name = "fecha_orden")
    private LocalDate fechaOrden;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "descuento")
    private Double descuento = 0.0;

    @Column(name = "igv")
    private Double igv;

    @Column(name = "total")
    private Double total;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoCompra estado;

    @Column(name = "codigo_seguimiento", unique = true, length = 50)
    private String codigoSeguimiento;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemCarrito> items = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCarroCompra", nullable = false)
    private CarroCompras carroCompras;

    public void addItem(ItemCarrito item) {
        items.add(item);
        item.setOrdenCompra(this);
    }

    public void removeItem(ItemCarrito item) {
        items.remove(item);
        item.setOrdenCompra(null);
    }
}
