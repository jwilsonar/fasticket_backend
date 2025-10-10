package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventosRepositorio extends JpaRepository<Evento, Integer> {
    
    List<Evento> findByActivoTrue();
    
    List<Evento> findByEstadoEventoAndActivoTrue(EstadoEvento estado);
    
    List<Evento> findByFechaEventoBetweenAndActivoTrue(LocalDate fechaInicio, LocalDate fechaFin);
    
    @Query("SELECT e FROM Evento e WHERE e.fechaEvento >= :fecha AND e.activo = true ORDER BY e.fechaEvento ASC")
    List<Evento> findEventosProximos(@Param("fecha") LocalDate fecha);
}
