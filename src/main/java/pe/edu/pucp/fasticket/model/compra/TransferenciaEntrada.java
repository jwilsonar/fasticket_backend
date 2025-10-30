package pe.edu.pucp.fasticket.model.compra;
import jakarta.persistence.*;
import lombok.Data;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de transferencia de un ticket.
 * Esta clase es central para manejar los estados PENDIENTE, ACEPTADA,
 * RECHAZADA y CANCELADA, cumpliendo con los RFs.
 */
@Entity
@Table(name = "transferencias")
@Data
public class TransferenciaEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Integer idTransferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_emisor", nullable = false)
    private Cliente emisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_receptor", nullable = false)
    private Cliente receptor;

    @Column(name = "fecha_transferencia", nullable = false)
    private LocalDateTime fechaTransferencia;

}