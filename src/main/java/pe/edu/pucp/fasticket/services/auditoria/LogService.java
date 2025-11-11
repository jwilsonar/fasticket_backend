package pe.edu.pucp.fasticket.services.auditoria;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;
import pe.edu.pucp.fasticket.repository.auditoria.ErrorLogRepository;
// Asumimos que tienes un DTO para la respuesta, si no, lo creamos
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import pe.edu.pucp.fasticket.mapper.ErrorLogMapper; // Asumimos un mapper

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final ErrorLogRepository errorLogRepository;
    private final ErrorLogMapper errorLogMapper; // Necesitaremos crear este mapper

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
}