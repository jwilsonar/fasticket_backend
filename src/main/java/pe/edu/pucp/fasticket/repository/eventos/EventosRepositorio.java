package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.Evento;

@Repository
public interface EventosRepositorio extends JpaRepository<Evento, Integer> {

}
