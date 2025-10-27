package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.Ticket;

@Repository
public interface TicketRepositorio extends JpaRepository<Ticket, Integer> {
    // Aquí puedes agregar consultas personalizadas
}