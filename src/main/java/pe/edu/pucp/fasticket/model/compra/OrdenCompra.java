package pe.edu.pucp.fasticket.model.compra;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import pe.edu.pucp.fasticket.model.fidelizacion.Canje;
import pe.edu.pucp.fasticket.model.pago.Pago;
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
    @Column(name = "idOrdenCompra")
    private Integer idOrdenCompra;  //

    @Column(name = "fechaOrden")
    private LocalDate fechaOrden; // 

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "descuentoPorMembrecia")
    private Double descuentoPorMembrecia = 0.0; // 

    @Column(name = "descuentoPorCanje")
    private Double descuentoPorCanje = 0.0; // 

    @Column(name = "igv")
    private Double igv;

    @Column(name = "total")
    private Double total;   //

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoCompra estado; //

    @Column(name = "codigoSeguimiento", unique = true, length = 50)
    private String codigoSeguimiento;

    @Column(name = "metodoPago", length = 50)
    private String metodoPago;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion")
    private LocalDate fechaCreacion;

    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion")
    private LocalDate fechaActualizacion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente; //

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemCarrito> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCarroCompra",nullable = true)
    private CarroCompras carroCompras;

    @Column(name = "fechaExpiracion")
    private LocalDateTime fechaExpiracion;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Canje> canjesAplicados = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "idPago", referencedColumnName = "idPago")
    private Pago pago;

    @Column(name = "descuento_promocional")
    private Double descuentoPromocional = 0.0;

    @Column(name = "codigo_promocional")
    private String codigoPromocionalAplicado;

    public void addItem(ItemCarrito item) {
        items.add(item);
        item.setOrdenCompra(this);
    }

    public void removeItem(ItemCarrito item) {
        items.remove(item);
        item.setOrdenCompra(null);
    }

    public void calcularTotal() {
        // 1. Calcular subtotal desde los ítems
        this.subtotal = this.items.stream()
                .mapToDouble(ItemCarrito::getPrecioFinal)
                .sum();
        // 2. Calcular IGV
        double valorVenta = this.subtotal / 1.18;
        this.igv = this.subtotal - valorVenta;
        // 3. Obtener descuentos seguros (evitar null)
        double descuentoPromo = this.descuentoPromocional != null ? this.descuentoPromocional : 0.0;
        double descuentoMembresia = this.descuentoPorMembrecia != null ? this.descuentoPorMembrecia : 0.0;
        double descuentoCanje = this.descuentoPorCanje != null ? this.descuentoPorCanje : 0.0;
        // 4. Si hay canje, total = 0 (regla de negocio)
        if (descuentoCanje > 0) {
            this.total = 0.0;
            return;
        }
        // 5. Total = subtotal - todos los descuentos
        this.total = this.subtotal - descuentoPromo - descuentoMembresia;
        // Evitar valores negativos
        if (this.total < 0) {
            this.total = 0.0;
        }
    }

    public void aplicarDescuentoYRecalcular() {
        calcularTotal();
    }
}
