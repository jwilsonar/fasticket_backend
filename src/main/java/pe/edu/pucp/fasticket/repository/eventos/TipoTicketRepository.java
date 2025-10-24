package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Import Modifying
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Repository
public interface TipoTicketRepository extends JpaRepository<TipoTicket, Integer> {

    // --- PASTED METHOD FROM TipoTicketRepositorio ---
    @Modifying
    @Query("UPDATE TipoTicket t SET t.cantidadDisponible = t.cantidadDisponible - :qty WHERE t.idTipoTicket = :id AND t.cantidadDisponible >= :qty")
    int decreaseStock(@Param("id") Integer id, @Param("qty") int qty);
    // --- END PASTED METHOD ---

    // Add any other custom queries for TipoTicket here if needed
}