package pe.edu.pucp.fasticket.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Configuración General", description = "Gestión de parámetros globales (Puntos, Límites, etc.)")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    /**
     * OPCIÓN 1: Listar todo (Usando tu Servicio con DTOs)
     * Recomendado para cargar la pantalla de configuración inicial.
     */
    @GetMapping
    @Operation(summary = "Listar configuraciones", description = "Obtiene todas las variables configurables del sistema.")
    public ResponseEntity<StandardResponse<List<ConfiguracionDTO>>> obtenerConfiguraciones() {
        log.info("GET /api/v1/configuracion");
        List<ConfiguracionDTO> configs = configuracionService.getAllConfiguraciones();
        return ResponseEntity.ok(StandardResponse.success("Configuraciones obtenidas", configs));
    }

    /**
     * OPCIÓN 2: Guardar todo en lote (Botón 'Guardar Cambios')
     * Recibe la lista completa y actualiza todo de golpe.
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar lote", description = "Actualiza múltiples configuraciones simultáneamente.")
    public ResponseEntity<StandardResponse<List<ConfiguracionDTO>>> actualizarConfiguraciones(
            @Valid @RequestBody List<ConfiguracionDTO> dtos) {
        log.info("PUT /api/v1/configuracion - Lote de {} items", dtos.size());
        List<ConfiguracionDTO> configsActualizadas = configuracionService.actualizarConfiguraciones(dtos);
        return ResponseEntity.ok(StandardResponse.success("Configuraciones actualizadas", configsActualizadas));
    }

    /**
     * OPCIÓN 3: Actualizar una sola clave (Edición rápida)
     * Útil si el front quiere guardar apenas cambias un input (auto-save).
     * Recibe ConfiguracionDTO en body (usa only 'value' y opcional 'descripcion').
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar individual", description = "Actualiza una sola variable por su clave.")
    public ResponseEntity<StandardResponse<ConfiguracionDTO>> actualizarConfiguracionIndividual(
            @PathVariable String key,
            @Valid @RequestBody ConfiguracionDTO dto) {

        log.info("PUT /api/v1/configuracion/{} - nuevo valor (masked) ", key);
        ConfiguracionDTO updated = configuracionService.actualizarPorKey(key, dto.getValue());
        return ResponseEntity.ok(StandardResponse.success("Configuración actualizada", updated));
    }
    @GetMapping("/{key}")
    @Operation(summary = "Obtener configuración por clave", description = "Obtiene una configuración específica por su clave.")
    public ResponseEntity<StandardResponse<ConfiguracionDTO>> obtenerConfiguracionPorClave(@PathVariable String key) {
        log.info("GET /api/v1/configuracion/{}", key);
        ConfiguracionDTO config = configuracionService.getConfiguracionPorClave(key);
        return ResponseEntity.ok(StandardResponse.success("Configuración obtenida", config));
    }
}