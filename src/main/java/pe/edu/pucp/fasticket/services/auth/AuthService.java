package pe.edu.pucp.fasticket.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.auth.*;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.security.CustomUserDetailsService;
import pe.edu.pucp.fasticket.security.JwtUtil;

import java.time.LocalDate;

/**
 * Servicio de autenticación y autorización.
 * Maneja login, registro y cambio de contraseña.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final PersonasRepositorio personasRepositorio;
    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final DistritoRepository distritoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Determina el rol del usuario basado en el dominio del email.
     * Los emails que terminen en @pucp.edu.pe serán ADMINISTRADOR,
     * todos los demás serán CLIENTE.
     */
    private Rol determinarRolPorEmail(String email) {
        if (email != null && email.toLowerCase().endsWith("@pucp.edu.pe")) {
            return Rol.ADMINISTRADOR;
        }
        return Rol.CLIENTE;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Intento de login para email: {}", request.getEmail());

        // Autenticar con Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getContrasena())
        );

        // Cargar usuario
        Persona persona = personasRepositorio.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!persona.getActivo()) {
            throw new BusinessException("La cuenta está desactivada");
        }

        // Generar token
        String token = jwtUtil.generateToken(persona.getEmail(), persona.getRol().name());

        log.info("Login exitoso para: {}", persona.getEmail());

        return LoginResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .idUsuario(persona.getIdPersona())
                .email(persona.getEmail())
                .nombreCompleto(persona.getNombres() + " " + persona.getApellidos())
                .rol(persona.getRol().name())
                .expiracion(86400000L) // 24 horas
                .build();
    }

    @Transactional
    public LoginResponseDTO registrarCliente(RegistroRequestDTO request) {
        log.info("Registro de nuevo usuario: {}", request.getEmail());

        // Validaciones
        if (personasRepositorio.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }

        if (personasRepositorio.existsByDocIdentidad(request.getDocIdentidad())) {
            throw new BusinessException("El documento de identidad ya está registrado");
        }

        // Determinar rol basado en el dominio del email
        Rol rol = determinarRolPorEmail(request.getEmail());
        log.info("Rol asignado para {}: {}", request.getEmail(), rol);

        // Buscar distrito si fue proporcionado
        Distrito distrito = null;
        if (request.getIdDistrito() != null) {
            distrito = distritoRepository.findById(request.getIdDistrito())
                    .orElse(null);
        }

        Persona personaGuardada;
        
        if (rol == Rol.ADMINISTRADOR) {
            // Crear administrador
            Administrador administrador = new Administrador();
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
            administrador.setCargo("Administrador del Sistema"); // Cargo por defecto
            administrador.setActivo(true);
            administrador.setFechaCreacion(LocalDate.now());

            personaGuardada = administradorRepository.save(administrador);
            log.info("Administrador registrado exitosamente: {}", personaGuardada.getEmail());
        } else {
            // Crear cliente
            Cliente cliente = new Cliente();
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

            personaGuardada = clienteRepository.save(cliente);
            log.info("Cliente registrado exitosamente: {}", personaGuardada.getEmail());
        }

        // Generar token automáticamente
        String token = jwtUtil.generateToken(personaGuardada.getEmail(), personaGuardada.getRol().name());

        return LoginResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .idUsuario(personaGuardada.getIdPersona())
                .email(personaGuardada.getEmail())
                .nombreCompleto(personaGuardada.getNombres() + " " + personaGuardada.getApellidos())
                .rol(personaGuardada.getRol().name())
                .expiracion(86400000L)
                .build();
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

