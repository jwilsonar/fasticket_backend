package pe.edu.pucp.fasticket.services.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.auth.CambioContrasenaDTO;
import pe.edu.pucp.fasticket.dto.auth.LoginRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.LoginResponseDTO;
import pe.edu.pucp.fasticket.dto.auth.RegistroRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.ResetPasswordByIdRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.ValidateCodeRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.auth.PasswordResetCode;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.notificaciones.TipoNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.repository.auth.PasswordResetCodeRepository;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.security.JwtUtil;
import pe.edu.pucp.fasticket.services.EmailService;
import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;
import pe.edu.pucp.fasticket.services.notificaciones.NotificationManager;
import pe.edu.pucp.fasticket.services.notificaciones.NotificationRequest;
import pe.edu.pucp.fasticket.services.notificaciones.PlantillaService;

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
    private final EmailService emailService; // SMTP legacy (fallback)
    private final pe.edu.pucp.fasticket.services.notificaciones.EmailService emailNotificacionesService; // Brevo
    private final PlantillaService plantillaService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final NotificationManager notificationManager;
    private final AuditLogService auditLogService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final int[] LOCK_TIME_DURATION = {0, 1, 15};
    private static final int N_MAX_ATTEMPTS = LOCK_TIME_DURATION.length;

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

    private String formatInstant(Instant instant) {
    return instant.atZone(ZoneId.systemDefault())
                  .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
}

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Intento de login para email: {}", request.getEmail());

        // Cargar usuario
        Persona persona = personasRepositorio.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        Instant now = Instant.now();

        // 1. Validar si está bloqueado PRIMERO
        if (persona.getLockedUntil() != null && persona.getLockedUntil().isAfter(now)) {
             String formattedDate = formatInstant(persona.getLockedUntil());
            throw new BusinessException("La cuenta está bloqueada hasta " + formattedDate);
        }

        // 2. Validar si está activo SEGUNDO
        if (!persona.getActivo()) {
            throw new BusinessException("La cuenta está desactivada");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getContrasena())
            );

            if (persona.getRol() == Rol.ADMINISTRADOR) {
                // Actualizar último acceso para administradores
                Administrador admin = (Administrador) persona;
                admin.setUltimoAcceso(now);
                administradorRepository.save(admin);
                String formattedDate = formatInstant(admin.getUltimoAcceso());
                log.info("Registrado último acceso para admin: {} - {}", persona.getEmail(), formattedDate);

                // --- INICIO RF-041: REGISTRO DE AUDITORÍA LOGIN ---
                auditLogService.registrarAuditoria(
                        admin,
                        "LOGIN",
                        "AuthService",
                        "Inicio de sesión exitoso."
                );
                // --- FIN RF-041 ---
            }
            
            // ÉXITO: Resetear intentos fallidos y bloqueo
            persona.setFailedAttempts(0);
            persona.setLockedUntil(null);
            personasRepositorio.save(persona);

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
                    .primerLogin(persona.getPrimerLogin())
                    .build();

        } catch (BadCredentialsException ex) {
            int attempts = (persona.getFailedAttempts() == null ? 0 : persona.getFailedAttempts()) + 1;
            persona.setFailedAttempts(attempts);

            if (attempts >= 1 && attempts <= N_MAX_ATTEMPTS) {
                int idx = attempts - 1;
                int minutes = LOCK_TIME_DURATION[idx];
                if (minutes > 0) {
                    Instant lockUntil = now.plus(Duration.ofMinutes(minutes));
                    persona.setLockedUntil(lockUntil);
                     String formattedDate = formatInstant(persona.getLockedUntil());
                    log.warn("Login fallido para {} (intentos={}). Bloqueo hasta {} ({} min)", 
                            persona.getEmail(), attempts, formattedDate, minutes);
                } else {
                    // minutes == 0: Solo contar intento, NO modificar lockedUntil
                    // Mantener el bloqueo actual si existe, o null si no hay bloqueo
                    if (attempts == 1) {
                        log.warn("Primer intento fallido para {}", persona.getEmail());
                    } else {
                        log.warn("Login fallido para {} (intentos={}), el próximo intento tendrá penalidad)", 
                                persona.getEmail(), attempts);
                    }
                }
            } else {
                persona.setFailedAttempts(0);
                persona.setLockedUntil(null); // Desbloquear la cuenta
                log.warn("Superó el límite de intentos para {}. Reseteando contador de intentos, tenga más cuidado la próxima vez.", 
                        persona.getEmail());
            }

            personasRepositorio.save(persona);
            throw new BadCredentialsException("Credenciales inválidas");
        }
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

            // Notificación de bienvenida/Verificación (email + in-app) usando NotificationManager
            try {
                String tokenVerificacion = jwtUtil.generateToken(personaGuardada.getEmail(), personaGuardada.getRol().name());
                String linkVerificacion = frontendUrl + "/verificar-cuenta?token=" + tokenVerificacion;
                Map<String,Object> params = new java.util.HashMap<>();
                params.put("nombre", personaGuardada.getNombres());
                params.put("linkVerificacion", linkVerificacion);

                NotificationRequest req = NotificationRequest.builder()
                    .personaId(personaGuardada.getIdPersona())
                    .email(personaGuardada.getEmail())
                    .nombre(personaGuardada.getNombres())
                    .notiTipo(TipoNotificacion.VERIFICACION_CUENTA)
                    .plantilla(TipoPlantilla.VERIFICAR_CUENTA)
                    .params(params)
                    .titulo("Verifica tu cuenta")
                    .mensaje("Hemos enviado un correo con el enlace de verificación.")
                    .build();
                notificationManager.notifyAllChannels(req);
            } catch (Exception e) {
                log.warn("Registro exitoso, pero falló la notificación de verificación: {}", e.getMessage());
            }

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

        // Cambio de la bandera (válido para ambos tipos de Usuario, útil solo para Administradores
        persona.setPrimerLogin(false);

        log.info("Contraseña cambiada exitosamente para usuario: {}", persona.getEmail());
    }

    
    @Transactional
    public void logout(String authHeader) {
        final String BEARER = "Bearer ";
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            throw new BusinessException("Token de autorización inválido");
        }

        String token = authHeader.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException("Token vacío");
        }

        try {
            String email = jwtUtil.extractUsername(token);

            // --- INICIO RF-041: REGISTRO DE AUDITORÍA LOGOUT ---
            // Necesitamos buscar al admin para pasarlo al log de auditoría
            personasRepositorio.findByEmail(email).ifPresent(persona -> {
                if (persona.getRol() == Rol.ADMINISTRADOR) {
                    auditLogService.registrarAuditoria(
                            (Administrador) persona,
                            "LOGOUT",
                            "AuthService",
                            "Cierre de sesión."
                    );
                }
            });
            // --- FIN RF-041 ---

            tokenBlacklistService.blacklistToken(token);
            log.info("Sesión cerrada exitosamente para: {}", email);
        } catch (io.jsonwebtoken.JwtException e) { // usar la excepción concreta del parser JWT
            log.warn("Token JWT inválido durante logout; procediendo a blacklist. Detalle:", e);
            tokenBlacklistService.blacklistToken(token);
        } catch (Exception e) {
            log.error("Error inesperado en logout:", e);
            throw e; // no silenciar errores inesperados
        }
    }

    @Transactional
    public void iniciarOlvidoContrasena(String email){
        log.info("Iniciar olvido de contraseña para: {}", email);
        Persona persona = personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Generar código 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));

        PasswordResetCode prc = new PasswordResetCode();
        prc.setPersonaId(persona.getIdPersona());
        prc.setEmail(email.toLowerCase());
        prc.setCodigo(codigo);
        prc.setExpiraEn(Instant.now().plus(Duration.ofMinutes(10)));
        prc.setVerificado(false);
        prc.setUsado(false);
        prc.setIntentos(0);
        passwordResetCodeRepository.save(prc);

        // Usar plantilla si existe (OLVIDO_CONTRASENA_CODIGO), con params
        Map<String, Object> params = new HashMap<>();
        params.put("nombre", persona.getNombres());
        params.put("codigo", codigo);
        params.put("email", email.toLowerCase());

        String asunto = "Código de recuperación de contraseña";
        String html;
        var plantilla = plantillaService.obtenerActiva(TipoPlantilla.OLVIDO_CONTRASENA_CODIGO);
        if (plantilla != null) {
            asunto = plantilla.getAsunto();
            html = plantillaService.render(plantilla.getHtml(), params);
        } else {
            html = "<h2>Tu código de verificación</h2>"
                 + "<p>Usa este código para continuar con el proceso de recuperación:</p>"
                 + "<p style='font-size:24px;letter-spacing:4px'><strong>" + codigo + "</strong></p>"
                 + "<p>El código expira en 10 minutos.</p>";
        }

        // Enviar por email + notificación in-app
        try {
            NotificationRequest req = NotificationRequest.builder()
                .personaId(persona.getIdPersona())
                .email(email)
                .nombre(persona.getNombres())
                .notiTipo(TipoNotificacion.RECUPERACION_CONTRASENA)
                .plantilla(TipoPlantilla.OLVIDO_CONTRASENA_CODIGO)
                .params(params)
                .subject(asunto)
                .html(html)
                .titulo("Recuperación de contraseña")
                .mensaje("Hemos enviado un código de verificación a tu correo.")
                .build();
            notificationManager.notifyAllChannels(req);
        } catch (Exception ex) {
            // fallback a SMTP si ocurre error en envío email
            emailService.enviarCorreoResetContrasena(email, asunto, html);
        }
    }

    @Transactional
    public void validarCodigoOlvido(ValidateCodeRequestDTO request) {
        String email = request.getEmail().toLowerCase();
        log.info("Validando código de olvido de contraseña para: {}", email);

        var opt = passwordResetCodeRepository.findTopByEmailOrderByIdDesc(email);
        PasswordResetCode prc = opt.orElseThrow(() -> new BusinessException("No hay solicitud vigente"));

        if (prc.isUsado()) throw new BusinessException("El código ya fue usado");
        if (Instant.now().isAfter(prc.getExpiraEn())) throw new BusinessException("El código ha expirado");
        if (!prc.getCodigo().equals(request.getCodigo())) {
            prc.setIntentos(prc.getIntentos() + 1);
            passwordResetCodeRepository.save(prc);
            throw new BusinessException("Código inválido");
        }
        prc.setVerificado(true);
        passwordResetCodeRepository.save(prc);
    }

    @Transactional
    public void resetearContrasenaPorId(ResetPasswordByIdRequestDTO request) {
        String email = request.getEmail().toLowerCase();
        log.info("Resetear contraseña por correo para usuario: {}", email);

        if (!request.getContrasena().equals(request.getContrasenaConfirmacion())) {
            throw new BusinessException("La contraseña y su confirmación no coinciden");
        }

        Persona persona = personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        var opt = passwordResetCodeRepository.findTopByEmailOrderByIdDesc(email);
        PasswordResetCode prc = opt.orElseThrow(() -> new BusinessException("No hay código validado para este usuario"));
        if (!prc.isVerificado()) throw new BusinessException("Debe validar el código primero");
        if (prc.isUsado()) throw new BusinessException("El código ya fue usado");
        if (Instant.now().isAfter(prc.getExpiraEn())) throw new BusinessException("El código ha expirado");

        persona.setContrasena(passwordEncoder.encode(request.getContrasena()));
        personasRepositorio.save(persona);

        prc.setUsado(true);
        passwordResetCodeRepository.save(prc);

        log.info("Contraseña reseteada correctamente para {}", persona.getEmail());
    }

}

