package pe.edu.pucp.fasticket.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.RegistroResponse;
import pe.edu.pucp.fasticket.dto.auth.*;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.security.JwtUtil;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import pe.edu.pucp.fasticket.events.ClienteRegistradoEvent;

/**
 * Servicio de autenticación y autorización.
 * Maneja login, registro y cambio de contraseña.
 * Implementa RF-048, RF-054, RF-055, RF-056, RF-062.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final PersonasRepositorio personasRepositorio;
    private final DistritoRepository distritoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Intento de login para email: {}", request.getEmail());

        // Autenticar con Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getContrasena())
        );

        // Cargar usuario
        Persona persona = personasRepositorio.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!persona.getActivo()) {
            throw new BusinessException("La cuenta está desactivada");
        }

        // Generar token JWT
        String token = jwtUtil.generateToken(persona.getEmail(), persona.getRol().name());

        log.info("Login exitoso para: {} con rol: {}", persona.getEmail(), persona.getRol());

        // Construir respuesta con token
        return LoginResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .idUsuario(persona.getIdPersona())
                .email(persona.getEmail())
                .nombreCompleto(persona.getNombres() + " " + persona.getApellidos())
                .rol(persona.getRol().name())
                .expiracion(86400000L) // 24 horas en milisegundos
                .build();
    }

    @Transactional
    public RegistroResponse registrarCliente(RegistroRequestDTO request) {
        log.info("Registro de nuevo cliente: {}", request.getEmail());

        // Validaciones
        if (personasRepositorio.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }

        if (personasRepositorio.existsByDocIdentidad(request.getDocIdentidad())) {
            throw new BusinessException("El documento de identidad ya está registrado");
        }

        // Buscar distrito si fue proporcionado
        Distrito distrito = null;
        if (request.getIdDistrito() != null) {
            distrito = distritoRepository.findById(request.getIdDistrito())
                    .orElse(null);
        }

        // Crear instancia según el dominio del email y configurar
        Persona personaGuardada;
        
        if (request.getEmail().toLowerCase().endsWith("@pucp.edu.pe")) {
            // Crear y configurar Administrador
            log.info("Registrando usuario con rol ADMINISTRADOR (email PUCP): {}", request.getEmail());
            Administrador administrador = new Administrador();
            
            // Configurar datos comunes (de Persona)
            administrador.setTipoDocumento(request.getTipoDocumento());
            administrador.setDocIdentidad(request.getDocIdentidad());
            administrador.setNombres(request.getNombres());
            administrador.setApellidos(request.getApellidos());
            administrador.setEmail(request.getEmail());
            administrador.setContrasena(passwordEncoder.encode(request.getContrasena()));
            administrador.setTelefono(request.getTelefono());
            administrador.setFechaNacimiento(request.getFechaNacimiento());
            administrador.setDireccion(request.getDireccion());
            administrador.setDistrito(distrito);
            administrador.setActivo(true);
            administrador.setFechaCreacion(LocalDate.now());
            
            // Configurar datos específicos de Administrador
            administrador.setCargo("Administrador");
            
            personaGuardada = personasRepositorio.save(administrador);
        } else {
            // Crear y configurar Cliente
            Cliente cliente = new Cliente();
            
            // Configurar datos comunes (de Persona)
            cliente.setTipoDocumento(request.getTipoDocumento());
            cliente.setDocIdentidad(request.getDocIdentidad());
            cliente.setNombres(request.getNombres());
            cliente.setApellidos(request.getApellidos());
            cliente.setEmail(request.getEmail());
            cliente.setContrasena(passwordEncoder.encode(request.getContrasena()));
            cliente.setTelefono(request.getTelefono());
            cliente.setFechaNacimiento(request.getFechaNacimiento());
            cliente.setDireccion(request.getDireccion());
            cliente.setDistrito(distrito);
            cliente.setActivo(true);
            cliente.setFechaCreacion(LocalDate.now());
            
            personaGuardada = personasRepositorio.save(cliente);
        }

        log.info("Usuario registrado exitosamente: {} con rol {}", personaGuardada.getEmail(), personaGuardada.getRol());

        // RF-048: Publicar evento para enviar email de verificación (Patrón Observer)
        if (personaGuardada instanceof Cliente) {
            try {
                String tokenVerificacion = UUID.randomUUID().toString();
                String nombreCompleto = personaGuardada.getNombres() + " " + personaGuardada.getApellidos();
                
                log.info("📢 Publicando evento ClienteRegistradoEvent para: {}", personaGuardada.getEmail());
                eventPublisher.publishEvent(
                    new ClienteRegistradoEvent(personaGuardada.getEmail(), nombreCompleto, tokenVerificacion)
                );
            } catch (Exception e) {
                log.error("⚠️ Error al publicar evento de registro (no crítico): {}", e.getMessage());
            }
        }

        return new RegistroResponse(personaGuardada.getEmail(), "Usuario registrado exitosamente", true);
    }

    @Transactional
    public void cambiarContrasena(Integer idUsuario, CambioContrasenaDTO request) {
        log.info("Cambio de contraseña para usuario ID: {}", idUsuario);

        Persona persona = personasRepositorio.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar contraseña actual
        if (!passwordEncoder.matches(request.getContrasenaActual(), persona.getContrasena())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        // Validar que nueva contraseña y confirmación coincidan
        if (!request.getContrasenaNueva().equals(request.getContrasenaConfirmacion())) {
            throw new BusinessException("La nueva contraseña y su confirmación no coinciden");
        }

        // Validar que la nueva contraseña sea diferente
        if (request.getContrasenaActual().equals(request.getContrasenaNueva())) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la actual");
        }

        // Cambiar contraseña
        persona.setContrasena(passwordEncoder.encode(request.getContrasenaNueva()));
        persona.setFechaActualizacion(LocalDate.now());
        personasRepositorio.save(persona);

        log.info("Contraseña cambiada exitosamente para usuario: {}", persona.getEmail());
    }
}

