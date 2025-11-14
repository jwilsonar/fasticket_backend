package pe.edu.pucp.fasticket.controllers.auditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.auditoria.AuditLogDTO;
import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/admin/audit") // Endpoint para auditoría
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Endpoint para consultar los logs de auditoría (RF-043).
     * Permite filtrar por rango de fechas, acción o ID de admin.
     */
    @GetMapping("/")
    public ResponseEntity<StandardResponse<List<AuditLogDTO>>> consultarLogsDeAuditoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) Integer idAdmin) {

        log.info("GET /api/v1/admin/audit - Filtros: inicio={}, fin={}, accion={}, idAdmin={}", inicio, fin, accion, idAdmin);

        List<AuditLogDTO> logs = auditLogService.consultarLogsDeAuditoria(inicio, fin, accion, idAdmin);

        return ResponseEntity.ok(StandardResponse.success("Logs de auditoría obtenidos", logs));
    }

    // --- NUEVO MÉTODO PARA EXPORTAR (RF-110) ---
    @GetMapping("/export")
    public ResponseEntity<String> exportarLogsDeAuditoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) Integer idAdmin) {

        log.info("Exportando logs de auditoría a CSV...");
        List<AuditLogDTO> logs = auditLogService.consultarLogsDeAuditoria(inicio, fin, accion, idAdmin);

        String csv = generarCsvDeAuditoria(logs);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=audit_logs.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    // Método helper para generar el CSV
    private String generarCsvDeAuditoria(List<AuditLogDTO> logs) {
        StringBuilder sb = new StringBuilder();
        // Encabezado
        sb.append("ID;FechaHora;AdminEmail;Accion;Modulo;Detalle\n");

        // Datos (Usamos punto y coma ;)
        logs.forEach(log -> {
            sb.append(log.getIdAudit()).append(";")
                    .append(log.getFechaHora()).append(";")
                    .append(limpiarParaCsv(log.getAdminEmail())).append(";")
                    .append(limpiarParaCsv(log.getAccion())).append(";")
                    .append(limpiarParaCsv(log.getModulo())).append(";")
                    .append(limpiarParaCsv(log.getDetalle())).append("\n");
        });
        return sb.toString();
    }

    // Helper para limpiar texto para CSV (copiado de LogController)
    private String limpiarParaCsv(String valor) {
        if (valor == null) return "";
        String limpio = valor.replace("\n", " ").replace("\r", " ");
        if (limpio.contains(";")) {
            return "\"" + limpio + "\"";
        }
        return limpio;
    }
}