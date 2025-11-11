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

            config.setValue(dto.getValue());
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