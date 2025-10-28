package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.domain.Pageable; // Necesario para limitar resultados
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Para la consulta personalizada
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

import java.util.List;

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
    @Query("SELECT t FROM Ticket t WHERE t.tipoTicket = :tipoTicket AND t.estado = :estado ORDER BY t.idTicket ASC")
    List<Ticket> findAvailableTicketsByTypeAndState(
            @Param("tipoTicket") TipoTicket tipoTicket,
            @Param("estado") EstadoTicket estado,
            Pageable pageable
    );
}
