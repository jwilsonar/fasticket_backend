package pe.edu.pucp.fasticket.services.auditoria;

import pe.edu.pucp.fasticket.dto.auditoria.AuditLogDTO; // NUEVO
import pe.edu.pucp.fasticket.model.auditoria.AuditLog;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import java.time.LocalDateTime; // NUEVO
import java.util.List; // NUEVO

public interface AuditLogService {

    void registrarAuditoria(Administrador admin, String accion, String modulo, String detalle);

    // --- MÉTODO NUEVO (Para RF-043) ---
    List<AuditLogDTO> consultarLogsDeAuditoria(LocalDateTime inicio, LocalDateTime fin, String accion, Integer idAdmin);
}