package pe.edu.pucp.fasticket.services.usuario;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.usuario.AdministradorPerfilResponseDTO;
import pe.edu.pucp.fasticket.dto.usuario.AdministradorPerfilUpdateDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Servicio para gestión de administradores.
 * Maneja perfiles y operaciones de administradores del sistema.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdministradorService {

    private final AdministradorRepository administradorRepository;
    private final PersonasRepositorio personasRepositorio;

    private final AuditLogService auditLogService;

    /**
     * Obtiene el perfil del administrador por email.
     * 
     * @param email Email del administrador
     * @return Perfil del administrador
     */
    public AdministradorPerfilResponseDTO obtenerPerfilPorEmail(String email) {
        log.info("Obteniendo perfil del administrador con email: {}", email);
        Administrador administrador = (Administrador) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con email: " + email));
        
        if (!(administrador instanceof Administrador)) {
            throw new ResourceNotFoundException("El usuario no es un administrador");
        }
        
        return convertirAPerfilDTO(administrador);
    }


    /**
     * Actualiza el perfil del administrador.
     * 
     * @param email Email del administrador autenticado
     * @param dto Datos a actualizar
     * @return Perfil actualizado
     */
    @Transactional
    public AdministradorPerfilResponseDTO actualizarPerfil(String email, AdministradorPerfilUpdateDTO dto) {
        log.info("Actualizando perfil del administrador: {}", email);
        
        Administrador administrador = (Administrador) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con email: " + email));

        // Actualizar campos si vienen en el DTO
        if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
            administrador.setNombres(dto.getNombres());
        }
        if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
            administrador.setApellidos(dto.getApellidos());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            administrador.setTelefono(dto.getTelefono());
        }
        if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {
            administrador.setDireccion(dto.getDireccion());
        }
        if (dto.getCargo() != null && !dto.getCargo().isBlank()) {
            administrador.setCargo(dto.getCargo());
        }
        
        // Validar que el nuevo email no esté en uso por otro usuario
        if (dto.getEmail() != null && !dto.getEmail().equals(administrador.getEmail())) {
            if (personasRepositorio.existsByEmail(dto.getEmail())) {
                throw new BusinessException("El email ya está registrado por otro usuario");
            }
            administrador.setEmail(dto.getEmail());
        }

        administrador.setFechaActualizacion(java.time.LocalDate.now());
        Administrador administradorActualizado = administradorRepository.save(administrador);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            // Como un admin solo puede actualizarse a sí mismo, el "admin" es el mismo
            String detalle = "Admin (ID: " + administrador.getIdPersona() + ") actualizó su propio perfil. Campos: Nombres, Apellidos, Email, etc.";
            auditLogService.registrarAuditoria(administrador, "ACTUALIZAR_PERFIL_ADMIN", "AdministradorService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (ACTUALIZAR_PERFIL_ADMIN): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Perfil actualizado exitosamente para: {}", email);
        return convertirAPerfilDTO(administradorActualizado);
    }


    /**
     * Convierte una entidad Administrador a AdministradorPerfilResponseDTO.
     * 
     * @param administrador Entidad administrador
     * @return DTO con información del perfil
     */
    private AdministradorPerfilResponseDTO convertirAPerfilDTO(Administrador administrador) {
        AdministradorPerfilResponseDTO dto = new AdministradorPerfilResponseDTO();
        dto.setIdAdministrador(administrador.getIdPersona());
        dto.setTipoDocumento(administrador.getTipoDocumento());
        dto.setDocIdentidad(administrador.getDocIdentidad());
        dto.setNombres(administrador.getNombres());
        dto.setApellidos(administrador.getApellidos());
        dto.setTelefono(administrador.getTelefono());
        dto.setEmail(administrador.getEmail());
        dto.setFechaNacimiento(administrador.getFechaNacimiento());
        dto.setDireccion(administrador.getDireccion());
        dto.setCargo(administrador.getCargo());
        dto.setEdad(administrador.calcularEdad());
        dto.setActivo(administrador.getActivo());
        return dto;
    }

    /**
     * NUEVO MÉTODO PARA RF-042: Desactivar cuenta de admin
     * Desactiva (borrado lógico) una cuenta de administrador.
     */
    @Transactional
    public void desactivarAdmin(Integer idAdminADesactivar) {
        log.warn("Solicitud de desactivación (borrado lógico) para admin ID: {}", idAdminADesactivar);

        Administrador adminActual = getAdminActual(); // El admin que realiza la acción

        if (adminActual.getIdPersona().equals(idAdminADesactivar)) {
            throw new BusinessException("Un administrador no puede desactivar su propia cuenta.");
        }

        Administrador adminADesactivar = administradorRepository.findById(idAdminADesactivar)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador a desactivar no encontrado con ID: " + idAdminADesactivar));

        if (!adminADesactivar.getActivo()) {
            throw new BusinessException("El administrador ya se encuentra desactivado.");
        }

        adminADesactivar.setActivo(false);
        administradorRepository.save(adminADesactivar);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "El Admin (ID: " + adminActual.getIdPersona() + ") desactivó la cuenta del Admin: " + adminADesactivar.getEmail() + " (ID: " + idAdminADesactivar + ")";
            auditLogService.registrarAuditoria(adminActual, "DESACTIVAR_ADMIN", "AdministradorService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (DESACTIVAR_ADMIN): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Admin ID: {} desactivado exitosamente por Admin ID: {}.", idAdminADesactivar, adminActual.getIdPersona());
    }

    // --- NUEVO MÉTODO HELPER PARA AUDITORÍA ---
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
