package pe.edu.pucp.fasticket.repository.fidelizacion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.fidelizacion.Puntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoTransaccion;

@Repository
public interface PuntosRepository extends JpaRepository<Puntos, Integer> {
    List<Puntos> findByCliente_IdPersona(Integer idCliente);
    List<Puntos> findByCliente_IdPersonaAndActivoTrue(Integer idCliente);
    
    @Query("SELECT p FROM Puntos p WHERE p.cliente.idPersona = :idCliente AND p.activo = true")
    List<Puntos> findActivePointsByCliente(@Param("idCliente") Integer idCliente);
    
    @Query("SELECT SUM(CASE WHEN p.tipoTransaccion = :tipoGanado THEN p.cantPuntos ELSE -p.cantPuntos END) " +
           "FROM Puntos p WHERE p.cliente.idPersona = :idCliente AND p.activo = true")
    Integer calcularPuntosAcumulados(@Param("idCliente") Integer idCliente, @Param("tipoGanado") TipoTransaccion tipoGanado);
}

