package pe.edu.pucp.fasticket.services.auditoria;

import pe.edu.pucp.fasticket.model.auditoria.AuditLog;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

public interface AuditLogService {

    /**
     * Registra una acción de auditoría (RF-109).
     * @param admin El admin que realiza la acción (puede ser nulo si es sistema)
     * @param accion La acción (ej. "CREAR_EVENTO")
     * @param modulo El servicio o módulo (ej. "EventoService")
     * @param detalle El detalle (ej. "ID del evento: 123")
     */
    void registrarAuditoria(Administrador admin, String accion, String modulo, String detalle);

    // Aquí irían los métodos para consultar (RF-110), pero los omito por brevedad
}