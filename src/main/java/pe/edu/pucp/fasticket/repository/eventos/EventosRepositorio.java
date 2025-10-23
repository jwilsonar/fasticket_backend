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
    
    /**
     * Encuentra todos los eventos activos.
     */
    List<Evento> findByActivoTrue();
    
    /**
     * RF-012: Filtra eventos por estado.
     */
    List<Evento> findByEstadoEventoAndActivoTrue(EstadoEvento estado);
    
    /**
     * RF-066: Filtra eventos por rango de fechas.
     */
    List<Evento> findByFechaEventoBetweenAndActivoTrue(LocalDate fechaInicio, LocalDate fechaFin);
    
    /**
     * RF-066: Encuentra eventos próximos (futuros).
     */
    @Query("SELECT e FROM Evento e WHERE e.fechaEvento >= :fecha AND e.activo = true ORDER BY e.fechaEvento ASC")
    List<Evento> findEventosProximos(@Param("fecha") LocalDate fecha);
    
    /**
     * RF-065: Filtra eventos por tipo/categoría.
     */
    List<Evento> findByTipoEventoAndActivoTrue(String tipoEvento);
    
    /**
     * RF-067: Filtra eventos por distrito del local.
     */
    List<Evento> findByLocalDistritoIdDistritoAndActivoTrue(Integer idDistrito);
    
    /**
     * RF-069: Lista eventos activos ordenados por fecha.
     */
    List<Evento> findByActivoTrueOrderByFechaEventoAsc();
}
