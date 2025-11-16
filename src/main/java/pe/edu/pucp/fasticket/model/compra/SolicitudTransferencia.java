package pe.edu.pucp.fasticket.model.compra;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_transferencia")
@Data
@NoArgsConstructor
public class SolicitudTransferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idSolicitud")
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTicket", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEmisor", nullable = false)
    private Cliente emisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idReceptor", nullable = false)
    private Cliente receptor;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @Column(name = "fechaSolicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "fechaRespuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "fechaExpiracion")
    private LocalDateTime fechaExpiracion;

    @Column(name = "activo")
    private Boolean activo = true;
}