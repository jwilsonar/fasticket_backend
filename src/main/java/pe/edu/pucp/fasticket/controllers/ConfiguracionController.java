package pe.edu.pucp.fasticket.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.ConfiguracionDTO;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.services.ConfiguracionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configuracion")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    /**
     * Obtiene todas las configuraciones globales del sistema.
     * (Para RF-044, RF-045, RF-046, RF-047)
     */
    @GetMapping
    public ResponseEntity<StandardResponse<List<ConfiguracionDTO>>> obtenerConfiguraciones() {
        log.info("GET /api/v1/admin/configuracion");
        List<ConfiguracionDTO> configs = configuracionService.getAllConfiguraciones();
        return ResponseEntity.ok(StandardResponse.success("Configuraciones obtenidas", configs));
    }

    /**
     * Actualiza un lote de configuraciones del sistema.
     * (Para RF-044, RF-045, RF-046, RF-047)
     */
    @PutMapping
    public ResponseEntity<StandardResponse<List<ConfiguracionDTO>>> actualizarConfiguraciones(
            @Valid @RequestBody List<ConfiguracionDTO> dtos) {

        log.info("PUT /api/v1/admin/configuracion - Actualizando {} claves", dtos.size());
        List<ConfiguracionDTO> configsActualizadas = configuracionService.actualizarConfiguraciones(dtos);
        return ResponseEntity.ok(StandardResponse.success("Configuraciones actualizadas", configsActualizadas));
    }
}