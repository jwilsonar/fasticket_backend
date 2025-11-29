package pe.edu.pucp.fasticket.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.ConfiguracionDTO;
import pe.edu.pucp.fasticket.mapper.ConfiguracionMapper;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;
    private final ConfiguracionMapper configuracionMapper;

    // --- Para Auditoría (RF-109) ---
    private final AuditLogService auditLogService;
    private final AdministradorRepository administradorRepository;

    /**
     * Obtiene todas las configuraciones del sistema.
     */
    public List<ConfiguracionDTO> getAllConfiguraciones() {
        log.info("Obteniendo todas las configuraciones del sistema");
        List<ConfiguracionGlobal> configs = configuracionRepository.findAll();
        return configuracionMapper.toDTOList(configs);
    }

    /**
     * Actualiza un conjunto de configuraciones.
     * RF-044, RF-045, RF-046, RF-047
     */
    @Transactional
    public List<ConfiguracionDTO> actualizarConfiguraciones(List<ConfiguracionDTO> dtos) {
        log.info("Actualizando {} configuraciones del sistema", dtos.size());
        Administrador adminActual = getAdminActual(); // Obtener admin para auditoría

        StringBuilder detallesAuditoria = new StringBuilder("Se actualizaron las configuraciones: ");

        for (ConfiguracionDTO dto : dtos) {
            ConfiguracionGlobal config = configuracionRepository.findById(dto.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada con key: " + dto.getKey()));

            // Guardar el valor anterior para la auditoría
            String valorAnterior = config.getValue();

            // Asignar el nuevo valor intentando mantener el tipo (INTEGER, DOUBLE, STRING)
            setTypedValue(config, dto.getValue());

            configuracionRepository.save(config);

            // Añadir al detalle de auditoría
            detallesAuditoria.append(String.format(" [%s: '%s' -> '%s'] ",
                    config.getKey(), valorAnterior, dto.getValue()));
        }

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            auditLogService.registrarAuditoria(adminActual, "ACTUALIZAR_CONFIG_GLOBAL", "ConfiguracionService", detallesAuditoria.toString());
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (ACTUALIZAR_CONFIG_GLOBAL): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        return getAllConfiguraciones();
    }

    /**
     * Actualiza una configuración por su clave y retorna el DTO actualizado.
     * Realiza auditoría de la acción.
     */
    @Transactional
    public ConfiguracionDTO actualizarPorKey(String key, String nuevoValor) {
        log.info("Actualizando configuración '{}' -> '{}'", key, nuevoValor);
        Administrador adminActual = getAdminActual();

        ConfiguracionGlobal config = configuracionRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada con key: " + key));

        String valorAnterior = config.getValue();

        // Asignar el nuevo valor intentando mantener el tipo (INTEGER, DOUBLE, STRING)
        setTypedValue(config, nuevoValor);

        ConfiguracionGlobal guardada = configuracionRepository.save(config);

        // Auditoría
        try {
            String detalle = String.format("Admin (ID: %d) actualizó configuración [%s: '%s' -> '%s']",
                    adminActual.getIdPersona(), key, valorAnterior, nuevoValor);
            auditLogService.registrarAuditoria(adminActual, "ACTUALIZAR_CONFIG_GLOBAL_POR_KEY", "ConfiguracionService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (ACTUALIZAR_CONFIG_GLOBAL_POR_KEY): {}", e.getMessage());
        }

        return configuracionMapper.toDTO(guardada);
    }

    /**
     * Intenta inferir el tipo del nuevo valor y asignarlo correctamente en la entidad.
     * Prioridad: INTEGER -> DOUBLE -> STRING.
     */
    private void setTypedValue(ConfiguracionGlobal config, String nuevoValor) {
        if (nuevoValor == null) {
            config.setValue((String) null);
            config.setValueType("STRING");
            return;
        }
        String trimmed = nuevoValor.trim();
        // intentar integer
        try {
            Integer intVal = Integer.valueOf(trimmed);
            config.setValue(String.valueOf(intVal));
            config.setValueType("INTEGER");
            return;
        } catch (Exception ignored) {}

        // intentar double
        try {
            Double dblVal = Double.valueOf(trimmed);
            config.setValue(String.valueOf(dblVal));
            config.setValueType("DOUBLE");
            return;
        } catch (Exception ignored) {}

        // por defecto guardar como string
        config.setValue(trimmed); // Lombok-generated setter for String field
        config.setValueType("STRING");
    }

    // --- Helper para obtener Admin (copiado de otros servicios) ---
    private Administrador getAdminActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No hay un usuario autenticado para la auditoría.");
        }
        String username = authentication.getName();
        return administradorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado para auditoría con username: " + username));
    }
}