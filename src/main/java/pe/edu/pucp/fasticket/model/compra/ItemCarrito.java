package pe.edu.pucp.fasticket.model.compra;

import java.time.LocalDate;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"carroCompra", "ordenCompra", "tickets"})
@ToString(exclude = {"carroCompra", "ordenCompra", "tickets"})
@Entity
@Table(name = "ItemCarrito")
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idItemCarrito")
    private Integer idItemCarrito;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "descuento")
    private Double descuento = 0.0;

    @Column(name = "precioFinal")
    private Double precioFinal;

    @Column(name = "fechaAgregado")
    private LocalDate fechaAgregado;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCarroCompra")
    private CarroCompras carroCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOrdenCompra")
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTipoTicket", nullable = false)
    private TipoTicket tipoTicket;

    @OneToMany(mappedBy = "itemCarrito", cascade = CascadeType.ALL, fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public void calcularPrecioFinal() {
        this.precioFinal = (this.precio * this.cantidad) - this.descuento;
    }

    public void addTicket(Ticket ticket) {
        if (this.tickets == null) {
            this.tickets = new ArrayList<>();
        }
        this.tickets.add(ticket);
        ticket.setItemCarrito(this);
        ticket.setOrdenCompra(this.ordenCompra);
    }

    public void setOrdenCompra(OrdenCompra ordenCompra) {
        this.ordenCompra = ordenCompra;
        if (this.tickets != null) {
            for (Ticket ticket : this.tickets) {
                ticket.setOrdenCompra(ordenCompra);
            }
        }
    }

    public void removeTicket(Ticket ticket) {
        if (this.tickets != null) {
            this.tickets.remove(ticket);
            ticket.setItemCarrito(null);
        }
    }
}
