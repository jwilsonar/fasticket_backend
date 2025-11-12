package pe.edu.pucp.fasticket.controllers.auditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import pe.edu.pucp.fasticket.services.auditoria.LogService;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDetalleDTO;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/logs") // Endpoint base para logs y auditoría de admin
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMINISTRADOR')") // Aseguramos que solo el Admin pueda ver
public class LogController {

    private final LogService logService;

    /**
     * Endpoint para consultar los logs de error (RF-108).
     * Permite filtrar por rango de fechas O por severidad.
     */

    @GetMapping("/errors")
    public ResponseEntity<StandardResponse<List<ErrorLogDTO>>> consultarLogsDeError(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) String severidad) {

        // --- INICIO DE PRUEBA TEMPORAL ---
        if (1 == 1) { // Condición simple para que se ejecute
            log.info("Forzando un error 500 para probar el GlobalExceptionHandler...");
            throw new RuntimeException("¡Prueba de error 500 inesperado!");
        }
        // --- FIN DE PRUEBA TEMPORAL (¡Borrar después!) ---

        log.info("GET /api/v1/admin/logs/errors - Filtros: inicio={}, fin={}, severidad={}", inicio, fin, severidad);

        List<ErrorLogDTO> logs = logService.consultarLogsDeError(inicio, fin, severidad);

        return ResponseEntity.ok(StandardResponse.success("Logs de error obtenidos", logs));
    }

    // --- NUEVO MÉTODO PARA EXPORTAR (RF-110) ---
    @GetMapping("/errors/export")
    public ResponseEntity<String> exportarLogsDeError(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) String severidad) {

        log.info("Exportando logs de error a CSV...");
        List<ErrorLogDTO> logs = logService.consultarLogsDeError(inicio, fin, severidad);

        String csv = generarCsvDeErrores(logs);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=error_logs.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    // Método helper para generar el CSV
    private String generarCsvDeErrores(List<ErrorLogDTO> logs) {
        StringBuilder sb = new StringBuilder();
        // Encabezado
        sb.append("ID;FechaHora;Severidad;Modulo;MensajeBreve;DetalleTecnico\n");

        // Datos (Usamos punto y coma ; para evitar problemas con comas en los mensajes)
        logs.forEach(log -> {
            sb.append(log.getIdError()).append(";")
                    .append(log.getFechaHora()).append(";")
                    .append(log.getSeveridad()).append(";")
                    .append(limpiarParaCsv(log.getModulo())).append(";")
                    .append(limpiarParaCsv(log.getMensajeBreve())).append(";")
                    .append(limpiarParaCsv(log.getDetalleTecnico())).append("\n");
        });
        return sb.toString();
    }

    // Helper para limpiar texto para CSV
    private String limpiarParaCsv(String valor) {
        if (valor == null) return "";
        // Quita saltos de línea y envuelve en comillas si es necesario
        String limpio = valor.replace("\n", " ").replace("\r", " ");
        if (limpio.contains(";")) {
            return "\"" + limpio + "\"";
        }
        return limpio;
    }

    @GetMapping("/errors/{id}")
    public ResponseEntity<StandardResponse<ErrorLogDetalleDTO>> consultarLogPorId(
            @PathVariable Integer id) {

        log.info("GET /api/v1/admin/logs/errors/{}", id);
        ErrorLogDetalleDTO logDetalle = logService.consultarLogDeErrorPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Detalle de log obtenido", logDetalle));
    }

}