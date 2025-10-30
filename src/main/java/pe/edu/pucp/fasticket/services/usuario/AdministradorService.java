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
}
