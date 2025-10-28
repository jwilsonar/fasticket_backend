package pe.edu.pucp.fasticket.model.eventos;

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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"evento", "tipoTicket", "itemCarrito", "cliente"})
@ToString(exclude = {"evento", "tipoTicket", "itemCarrito", "cliente"})
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
    @JoinColumn(name = "idTipoTicket")
    private TipoTicket tipoTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idItemCarrito",nullable = true)
    private ItemCarrito itemCarrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden_compra")
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    @Column(name = "nombreAsistente", length = 100)
    private String nombreAsistente;

    @Column(name = "apellidoAsistente", length = 100)
    private String apellidoAsistente;

    @Column(name = "documentoAsistente", length = 20)
    private String documentoAsistente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoDocumentoAsistente", length = 100)
    private TipoDocumento tipoDocumentoAsistente;



}
