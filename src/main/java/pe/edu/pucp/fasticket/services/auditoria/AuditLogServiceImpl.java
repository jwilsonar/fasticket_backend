package pe.edu.pucp.fasticket.services.auditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.auditoria.AuditLogDTO;
import pe.edu.pucp.fasticket.mapper.AuditLogMapper;
import pe.edu.pucp.fasticket.model.auditoria.AuditLog;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.repository.auditoria.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional // Sin readOnly, es una escritura
    public void registrarAuditoria(Administrador admin, String accion, String modulo, String detalle) {
        try {
            AuditLog log = new AuditLog();
            log.setFechaHora(LocalDateTime.now());
            log.setAdministrador(admin);
            log.setAccion(accion);
            log.setModulo(modulo);
            log.setDetalle(detalle);
            // log.setIpUsuario(null); // Podríamos obtener la IP del request si lo pasamos

            auditLogRepository.save(log);

        } catch (Exception e) {
            // No queremos que un fallo en la auditoría rompa la operación principal
            log.error("CRÍTICO: Fallo al guardar log de auditoría. Acción: {}", accion, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> consultarLogsDeAuditoria(LocalDateTime inicio, LocalDateTime fin, String accion, Integer idAdmin) {
        List<AuditLog> logs;

        if (inicio != null && fin != null) {
            logs = auditLogRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
        } else if (accion != null && !accion.isBlank()) {
            logs = auditLogRepository.findByAccionOrderByFechaHoraDesc(accion);
        } else if (idAdmin != null) {
            logs = auditLogRepository.findByAdministradorIdPersonaOrderByFechaHoraDesc(idAdmin);
        } else {
            logs = auditLogRepository.findAll(); // Podrías limitar esto con un Pageable
        }

        return logs.stream()
                .map(auditLogMapper::toDTO)
                .collect(Collectors.toList());
    }
}