package pe.edu.pucp.fasticket.repository.eventos;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;

/**
 * Repositorio para la entidad Evento.
 * Implementa queries personalizadas para búsqueda y filtrado según requerimientos funcionales.
 */
@Repository
public interface EventosRepositorio extends JpaRepository<Evento, Integer> {
    
    List<Evento> findByActivoTrue();
    
    List<Evento> findByEstadoEventoAndActivoTrue(EstadoEvento estado);
    
    List<Evento> findByFechaEventoBetweenAndActivoTrue(LocalDate fechaInicio, LocalDate fechaFin);
    
    @Query("SELECT e FROM Evento e WHERE e.fechaEvento >= :fecha AND e.activo = true ORDER BY e.fechaEvento ASC")
    List<Evento> findEventosProximos(@Param("fecha") LocalDate fecha);
}
