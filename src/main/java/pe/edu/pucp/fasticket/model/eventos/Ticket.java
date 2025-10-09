package pe.edu.pucp.fasticket.model.eventos;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"evento", "categoriaEntrada", "clienteActual", "itemsCarrito"})
@ToString(exclude = {"evento", "categoriaEntrada", "clienteActual", "itemsCarrito"})
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Integer idTicket;

    @Column(name = "codigo_qr", unique = true, length = 255)
    private String codigoQr;

    @Lob
    @Column(name = "qr_image")
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

    @Column(name = "usuario_creacion")
    private Integer usuarioCreacion;

    @Column(name = "fecha_creacion")
    private java.time.LocalDate fechaCreacion;

    @Column(name = "usuario_actualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fecha_actualizacion")
    private java.time.LocalDate fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria_entrada", nullable = false)
    private CategoriaEntrada categoriaEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_actual")
    private Cliente clienteActual;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemCarrito> itemsCarrito = new ArrayList<>();
}
