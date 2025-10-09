package pe.edu.pucp.fasticket.model.eventos;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"evento", "categoriaEntrada", "clienteActual", "itemCarrito"})
@ToString(exclude = {"evento", "categoriaEntrada", "clienteActual", "itemCarrito"})
@Entity
@Table(name = "Ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTicket")
    private Integer idTicket;

    @Column(name = "codigoQr", unique = true, length = 255)
    private String codigoQr;

    @Lob
    @Column(name = "qrImage")
    private byte[] qrImage;

    @Column(name = "asiento", length = 50)
    private String asiento;

    @Column(name = "fila", length = 50)
    private String fila;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoTicket estado;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion")
    private java.time.LocalDate fechaCreacion;

    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion")
    private java.time.LocalDate fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEvento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idItemCarrito", nullable = false)
    private ItemCarrito itemCarrito;
}
