package pe.edu.pucp.fasticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Repository
public interface TipoTicketRepository extends JpaRepository<TipoTicket, Integer> {

}