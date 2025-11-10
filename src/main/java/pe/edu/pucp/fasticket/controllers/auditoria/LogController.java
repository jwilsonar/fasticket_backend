package pe.edu.pucp.fasticket.controllers.auditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDTO;
import pe.edu.pucp.fasticket.services.auditoria.LogService;

import java.time.LocalDateTime;
import java.util.List;

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

}