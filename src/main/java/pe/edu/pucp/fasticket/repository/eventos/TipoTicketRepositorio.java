package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Repository
public interface TipoTicketRepositorio extends JpaRepository<TipoTicket, Integer> {
    @Modifying
    @Query("UPDATE TipoTicket t SET t.cantidadDisponible = t.cantidadDisponible - :qty WHERE t.idTipoTicket = :id AND t.cantidadDisponible >= :qty")
    int decreaseStock(@Param("id") Integer id, @Param("qty") int qty);
}
