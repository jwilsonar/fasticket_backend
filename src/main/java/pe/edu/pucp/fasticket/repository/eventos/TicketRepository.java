package pe.edu.pucp.fasticket.repository.eventos;

import java.time.LocalDate;
import java.util.List; // Necesario para limitar resultados

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository; // Para la consulta personalizada
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    /**
     * Busca los primeros 'count' tickets disponibles para un tipo de ticket específico.
     * Es crucial para la lógica de reserva de inventario.
     * Nota: Esta implementación usa JPQL para asegurar que se seleccionen
     * los tickets correctos antes de limitar.
     *
     * @param tipoTicket El tipo de ticket a buscar.
     * @param estado El estado deseado (ej: DISPONIBLE).
     * @param pageable Objeto Pageable para limitar los resultados (ej: PageRequest.of(0, count)).
     * @return Una lista de tickets disponibles, limitada a la cantidad solicitada.
     */
    @Query("SELECT t FROM Ticket t WHERE t.tipoTicket = :tipoTicket AND t.estado = :estado AND t.activo = true ORDER BY t.idTicket ASC")
    List<Ticket> findAvailableTicketsByTypeAndState(
            @Param("tipoTicket") TipoTicket tipoTicket,
            @Param("estado") EstadoTicket estado,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.cliente.idPersona = :idCliente AND t.tipoTicket.idTipoTicket = :idTipoTicket AND t.estado IN ('VENDIDA', 'RESERVADA')")
    Integer countTicketsByClienteAndTipoTicket(@Param("idCliente") Integer idCliente, @Param("idTipoTicket") Integer idTipoTicket);

    /**
     * Busca un ticket usando su código QR único.
     * Es la base para la validación de entradas (RF-094).
     */
    Optional<Ticket> findByCodigoQr(String codigoQr);

    @Query("SELECT t FROM Ticket t " +
            "WHERE t.cliente.idPersona = :idCliente " +
            "AND t.estado = 'VENDIDA' " +
            "AND t.activo = true " +
            "AND t.evento.fechaEvento >= :fechaHoy " +
            "ORDER BY t.evento.fechaEvento ASC")
    List<Ticket> findTicketsTransferiblesByCliente(
            @Param("idCliente") Integer idCliente,
            @Param("fechaHoy") LocalDate fechaHoy
    );

    @Query("SELECT t.idTicket FROM Ticket t WHERE t.itemCarrito.idItemCarrito = :itemId")
    List<Integer> findTicketIdsByItemCarritoId(@Param("itemId") Integer itemId);

    @Query("SELECT t FROM Ticket t WHERE t.ordenCompra.idOrdenCompra = :idOrdenCompra AND t.activo = true")
    List<Ticket> findByOrdenCompraId(@Param("idOrdenCompra") Integer idOrdenCompra);
}
