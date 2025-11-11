package pe.edu.pucp.fasticket.repository.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Integer> {

    /**
     * Busca errores por severidad, ordenados por fecha descendente.
     * (Parte de RF-108)
     */
    List<ErrorLog> findBySeveridadOrderByFechaHoraDesc(String severidad);

    /**
     * Busca errores dentro de un rango de fechas, ordenados por fecha descendente.
     * (Parte de RF-108)
     */
    List<ErrorLog> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);
}