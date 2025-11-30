package pe.edu.pucp.fasticket.repository.eventos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Repository
public interface TipoTicketRepositorio extends JpaRepository<TipoTicket, Integer> {
    
    @Modifying
    @Query("UPDATE TipoTicket t SET t.cantidadDisponible = t.cantidadDisponible - :qty WHERE t.idTipoTicket = :id AND t.cantidadDisponible >= :qty")
    int decreaseStock(@Param("id") Integer id, @Param("qty") int qty);
    
    List<TipoTicket> findByZonaIdZona(Integer idZona);
    
    boolean existsByNombreAndZonaIdZona(String nombre, Integer idZona);
    
    List<TipoTicket> findByZonaIdZonaAndActivoTrue(Integer idZona);
    
    @Query("SELECT DISTINCT t.evento FROM Ticket t WHERE t.tipoTicket.idTipoTicket = :idTipoTicket")
    Optional<Evento> findEventoByTipoTicket(@Param("idTipoTicket") Integer idTipoTicket);

    // Estadísticas por evento para el dashboard

    @Query("SELECT t.evento.id, SUM(t.cantidadVendida) FROM TipoTicket t GROUP BY t.evento.id")
    Map<Integer, Integer> findTotalVendidoPorEvento();

    @Query("SELECT COALESCE((SELECT SUM(tt.precio * tt.cantidadVendida) FROM TipoTicket tt WHERE tt.evento.idEvento = :idEvento AND tt.activo = true), 0)")
    Double sumIngresosByEventoId(@Param("idEvento") Integer idEvento);
}