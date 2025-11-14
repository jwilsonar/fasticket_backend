package pe.edu.pucp.fasticket.services.auditoria;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;
import pe.edu.pucp.fasticket.repository.auditoria.ErrorLogRepository;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import pe.edu.pucp.fasticket.mapper.ErrorLogMapper;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDetalleDTO;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;
import pe.edu.pucp.fasticket.repository.auditoria.ErrorLogRepository;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import java.util.NoSuchElementException;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final ErrorLogRepository errorLogRepository;
    private final ErrorLogMapper errorLogMapper;
    private final AdministradorRepository administradorRepository;

    /**
     * Guarda un error en la base de datos (RF-107).
     * Este método se llama desde el GlobalExceptionHandler.
     */
    @Transactional // Sin readOnly, ya que escribe en la BD
    public void registrarError(ErrorLog errorLog) {
        try {
            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            // Si falla el guardado del log, solo lo imprimimos en consola
            // para no causar un bucle de errores.
            System.err.println("Error CRÍTICO: No se pudo guardar el log de error en la BD.");
            e.printStackTrace();
        }
    }

    // ----- AÑADIR TODO ESTE MÉTODO NUEVO -----
    /**
     * Registra un error manual enviado desde el formulario.
     */
    @Transactional
    public ErrorLogDetalleDTO registrarErrorManual(ErrorLogRequestDTO requestDTO, String adminEmail) {

        // 1. Buscamos al admin que está registrando el error
        // (Asumo que tu AdministradorRepository tiene 'findByEmail')
        Administrador admin = administradorRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new NoSuchElementException("Administrador no encontrado: " + adminEmail));

        // 2. Mapeamos el DTO de solicitud a la Entidad
        // (Asumo que tu ErrorLogMapper tiene este método, si no, lo creas.
        // O lo hacemos manual)

        // --- Opción Manual (más segura si no quieres tocar el Mapper) ---
        ErrorLog nuevoError = new ErrorLog();
        nuevoError.setFechaHora(requestDTO.getFechaHora());
        nuevoError.setSeveridad(requestDTO.getSeveridad());
        nuevoError.setModulo(requestDTO.getModulo());
        nuevoError.setMensajeBreve(requestDTO.getMensajeBreve());
        nuevoError.setDetalleTecnico(requestDTO.getDetalleTecnico());
        nuevoError.setTraza(requestDTO.getTraza());

        // Asignamos al admin que encontramos
        // ASUMO que tu entidad ErrorLog tiene un campo 'setAdministrador'
        // Si no, tu mapper 'toDetalleDTO' no podría sacar el nombre.
        nuevoError.setAdministrador(admin);

        // 3. Guardamos la entidad en la BD
        ErrorLog errorGuardado = errorLogRepository.save(nuevoError);

        // 4. Mapeamos la entidad guardada al DTO de respuesta (el que tiene el 'nombreAdmin')
        return errorLogMapper.toDetalleDTO(errorGuardado);
    }

    /**
     * Consulta los errores (RF-108).
     */
    @Transactional(readOnly = true)
    public List<ErrorLogDTO> consultarLogsDeError(LocalDateTime inicio, LocalDateTime fin, String severidad) {
        List<ErrorLog> logs;
        if (severidad != null && !severidad.isBlank()) {
            logs = errorLogRepository.findBySeveridadOrderByFechaHoraDesc(severidad);
        } else if (inicio != null && fin != null) {
            logs = errorLogRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
        } else {
            logs = errorLogRepository.findAll(); // Devuelve todos si no hay filtro
        }

        return logs.stream()
                .map(errorLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ErrorLogDetalleDTO consultarLogDeErrorPorId(Integer id) {
        ErrorLog log = errorLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log de error no encontrado con ID: " + id));

        return errorLogMapper.toDetalleDTO(log);
    }
}