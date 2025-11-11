package pe.edu.pucp.fasticket.repository.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.fasticket.model.auditoria.AuditLog;
import java.util.List;
import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    // Para RF-110 (Consultar y filtrar)
    List<AuditLog> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);
    List<AuditLog> findByAccionOrderByFechaHoraDesc(String accion);
    List<AuditLog> findByAdministradorIdPersonaOrderByFechaHoraDesc(Integer idAdmin);
}