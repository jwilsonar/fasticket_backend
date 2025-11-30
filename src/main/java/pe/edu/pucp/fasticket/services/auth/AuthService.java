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
    private final PlantillaService plantillaService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final NotificationManager notificationManager;
    private final AuditLogService auditLogService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final int[] LOCK_TIME_DURATION = {0, 1, 15};
    private static final int N_MAX_ATTEMPTS = LOCK_TIME_DURATION.length;

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

        // 3. Validar si la cuenta está verificada (solo para Clientes)
        if (persona.getRol() == Rol.CLIENTE) {
            Cliente cliente = clienteRepository.findByEmail(persona.getEmail())
                    .orElseThrow(() -> new BusinessException("Error al cargar información del cliente"));
            
            if (!Boolean.TRUE.equals(cliente.getVerificado())) {
                log.warn("Intento de login con cuenta no verificada: {}", persona.getEmail());
                throw new BusinessException("Tu cuenta no ha sido verificada. Por favor, verifica tu correo electrónico haciendo clic en el enlace que te enviamos al registrarte. Si no recibiste el correo, puedes solicitar uno nuevo.");
            }
        }

        try {
            authenticationManager.authenticate(
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
            int attempts = persona.getFailedAttempts() != null ? persona.getFailedAttempts() + 1 : 1;
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

        // 1. Validaciones
        if (personasRepositorio.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }
        if (personasRepositorio.existsByDocIdentidad(request.getDocIdentidad())) {
            throw new BusinessException("El documento de identidad ya está registrado");
        }

        // 2. Buscar distrito
        Distrito distrito = null;
        if (request.getIdDistrito() != null) {
            distrito = distritoRepository.findById(request.getIdDistrito()).orElse(null);
        }

        // 3. SIEMPRE CREAR COMO CLIENTE (Eliminamos el if/else de roles)
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

        // Configuración por defecto
        cliente.setActivo(true);
        cliente.setRol(Rol.CLIENTE); // Forzamos rol Cliente
        cliente.setFechaCreacion(LocalDate.now());
        cliente.setVerificado(false); // Iniciar como no verificado

        Persona personaGuardada = clienteRepository.save(cliente);

        // 4. Enviar correo de verificación
        try {
            log.info("📧 Generando token de verificación para: {}", personaGuardada.getEmail());
            String tokenVerificacion = jwtUtil.generateVerificationToken(personaGuardada.getEmail());
            
            // Construir URL completa: FRONTEND_URL + /verificar-cuenta/ + token
            String linkVerificacion = frontendUrl.endsWith("/") 
                ? frontendUrl + "verificar-cuenta/" + tokenVerificacion
                : frontendUrl + "/verificar-cuenta/" + tokenVerificacion;
            
            log.info("🔗 Link de verificación generado para: {}", personaGuardada.getEmail());
            log.debug("Link de verificación (oculto en producción): {}***", linkVerificacion.substring(0, Math.min(50, linkVerificacion.length())));

            Map<String, Object> params = new HashMap<>();
            params.put("nombre", personaGuardada.getNombres());
            params.put("linkVerificacion", linkVerificacion);
            params.put("email", personaGuardada.getEmail());

            NotificationRequest req = NotificationRequest.builder()
                    .personaId(personaGuardada.getIdPersona())
                    .email(personaGuardada.getEmail())
                    .nombre(personaGuardada.getNombres())
                    .notiTipo(TipoNotificacion.VERIFICACION_CUENTA)
                    .plantilla(TipoPlantilla.VERIFICAR_CUENTA)
                    .params(params)
                    .titulo("Verifica tu cuenta")
                    .mensaje("Por favor, verifica tu correo electrónico para activar tu cuenta.")
                    .sendEmail(true)
                    .sendInApp(false) // No enviar notificación in-app para nuevos registros
                    .build();
                    
            notificationManager.notifyAllChannels(req);
            log.info("✅ Correo de verificación enviado exitosamente a: {}", personaGuardada.getEmail());

        } catch (Exception e) {
            log.error("❌ Error enviando correo de verificación a {}: {}", personaGuardada.getEmail(), e.getMessage(), e);
            // No fallar el registro si el correo no se envía
            log.warn("⚠️ El usuario fue registrado pero no se pudo enviar el correo de verificación");
        }

        log.info("Cliente registrado exitosamente: {}", personaGuardada.getEmail());

        // 5. Retornar respuesta
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

    /**
     * Verifica si existe un usuario con el email dado.
     * @param email Email a verificar
     * @return true si el usuario existe, false en caso contrario
     */
    public boolean existeUsuarioPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return personasRepositorio.findByEmail(email.toLowerCase()).isPresent();
    }

    @Transactional
    public void iniciarOlvidoContrasena(String email){
        log.info("🔐 Iniciar olvido de contraseña para: {}", email);
        
        // Buscar usuario (opcional - puede no existir)
        Persona persona = personasRepositorio.findByEmail(email).orElse(null);
        
        Integer personaId = null;
        String nombreUsuario = "Usuario";
        
        if (persona != null) {
            personaId = persona.getIdPersona();
            nombreUsuario = persona.getNombres() != null && !persona.getNombres().isBlank() 
                ? persona.getNombres() 
                : "Usuario";
            log.info("✅ Usuario encontrado: {} (ID: {})", persona.getEmail(), personaId);
        } else {
            log.info("ℹ️ Usuario no encontrado para email: {}, se enviará correo de todas formas", email);
        }

        // Generar código 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));
        log.debug("📝 Código de recuperación generado para: {}", email);

        // Guardar código de recuperación (incluso si el usuario no existe)
        // Si el usuario no existe, personaId será null (registro temporal)
        PasswordResetCode prc = new PasswordResetCode();
        prc.setPersonaId(personaId); // Puede ser null si el usuario no existe (registro temporal)
        prc.setEmail(email.toLowerCase());
        prc.setCodigo(codigo);
        prc.setExpiraEn(Instant.now().plus(Duration.ofMinutes(10)));
        prc.setVerificado(false);
        prc.setUsado(false);
        prc.setIntentos(0);
        
        try {
            passwordResetCodeRepository.save(prc);
            log.debug("💾 Código de recuperación guardado en BD (personaId={})", personaId != null ? personaId : "null (temporal)");
        } catch (Exception e) {
            // Si falla por restricción NOT NULL en BD, informar al usuario
            if (e.getMessage() != null && e.getMessage().contains("null value in column \"persona_id\"")) {
                log.error("❌ Error: La columna persona_id en password_reset_codes tiene restricción NOT NULL en la BD.");
                log.error("❌ Ejecuta el script: src/main/resources/sql/alter_password_reset_codes_persona_id_nullable.sql");
                throw new BusinessException("Error de configuración de base de datos. Contacte al administrador.");
            }
            throw e;
        }

        // Usar plantilla si existe (OLVIDO_CONTRASENA_CODIGO), con params
        Map<String, Object> params = new HashMap<>();
        params.put("nombre", nombreUsuario);
        params.put("codigo", codigo);
        params.put("email", email.toLowerCase());

        String asunto = "Código de recuperación de contraseña";
        String html;
        var plantilla = plantillaService.obtenerActiva(TipoPlantilla.OLVIDO_CONTRASENA_CODIGO);
        if (plantilla != null) {
            asunto = plantilla.getAsunto();
            html = plantillaService.render(plantilla.getHtml(), params);
            log.debug("📄 Usando plantilla personalizada para email de recuperación");
        } else {
            html = "<h2>Tu código de verificación</h2>"
                 + "<p>Usa este código para continuar con el proceso de recuperación:</p>"
                 + "<p style='font-size:24px;letter-spacing:4px'><strong>" + codigo + "</strong></p>"
                 + "<p>El código expira en 10 minutos.</p>";
            log.debug("📄 Usando plantilla por defecto para email de recuperación");
        }

        // Enviar por email (siempre se envía, incluso si el usuario no existe)
        boolean emailEnviado = false;
        try {
            log.info("📧 Intentando enviar correo de recuperación a: {}", email);
            log.debug("📋 Configuración: Usuario existe={}, Nombre={}, Código generado", personaId != null, nombreUsuario);
            NotificationRequest req = NotificationRequest.builder()
                .personaId(personaId) // Puede ser null si el usuario no existe
                .email(email)
                .nombre(nombreUsuario)
                .notiTipo(TipoNotificacion.RECUPERACION_CONTRASENA)
                .plantilla(TipoPlantilla.OLVIDO_CONTRASENA_CODIGO)
                .params(params)
                .subject(asunto)
                .html(html)
                .titulo("Recuperación de contraseña")
                .mensaje("Hemos enviado un código de verificación a tu correo.")
                .sendInApp(false) // No enviar notificación in-app si el usuario no existe
                .build();
            notificationManager.notifyAllChannels(req);
            emailEnviado = true;
            log.info("✅ Notificación enviada a través de NotificationManager");
        } catch (Exception ex) {
            log.warn("⚠️ Error al enviar notificación a través de NotificationManager: {}", ex.getMessage());
            log.debug("Detalles del error:", ex);
            // fallback a SMTP directo si ocurre error en envío email (Brevo falló o no está configurado)
            try {
                log.info("🔄 Intentando fallback a EmailService (SMTP directo, sin Brevo)");
                emailService.enviarCorreoResetContrasena(email, asunto, html);
                emailEnviado = true;
                log.info("✅ Email enviado exitosamente mediante fallback a SMTP directo");
            } catch (Exception fallbackEx) {
                log.error("❌ Error crítico: No se pudo enviar el correo ni por NotificationManager (Brevo) ni por fallback (SMTP): {}", fallbackEx.getMessage(), fallbackEx);
                throw new BusinessException("No se pudo enviar el correo de recuperación. Por favor, intente más tarde.");
            }
        }
        
        if (!emailEnviado) {
            log.error("❌ El correo no se pudo enviar para: {}", email);
            throw new BusinessException("No se pudo enviar el correo de recuperación. Por favor, intente más tarde.");
        }
        
        log.info("✅ Proceso de olvido de contraseña completado para: {} (correo enviado)", email);
    }

    /**
     * Valida el código de recuperación de contraseña.
     * Verifica que el código sea correcto, no haya sido usado, no haya expirado
     * y no se hayan excedido los intentos máximos.
     * 
     * @param request Contiene email y código a validar
     * @throws BusinessException si el código es inválido, expirado, usado o se excedieron los intentos
     */
    @Transactional
    public void validarCodigoOlvido(ValidateCodeRequestDTO request) {
        String email = request.getEmail().toLowerCase();
        log.info("🔐 Validando código de recuperación para: {}", email);

        // Buscar el código más reciente para este email
        var opt = passwordResetCodeRepository.findTopByEmailOrderByIdDesc(email);
        PasswordResetCode prc = opt.orElseThrow(() -> {
            log.warn("⚠️ No hay solicitud de recuperación vigente para: {}", email);
            return new BusinessException("No hay una solicitud de recuperación vigente para este correo");
        });

        // Validar que no haya sido usado
        if (prc.isUsado()) {
            log.warn("⚠️ Intento de usar código ya utilizado para: {}", email);
            throw new BusinessException("Este código ya fue utilizado. Por favor, solicita uno nuevo.");
        }

        // Validar que no haya expirado
        if (Instant.now().isAfter(prc.getExpiraEn())) {
            log.warn("⚠️ Código expirado para: {}", email);
            throw new BusinessException("El código ha expirado. Por favor, solicita uno nuevo.");
        }

        // Validar intentos máximos
        final int MAX_INTENTOS = 5;
        if (prc.getIntentos() >= MAX_INTENTOS) {
            log.warn("⚠️ Se excedieron los intentos máximos para: {}", email);
            throw new BusinessException("Se excedieron los intentos máximos. Por favor, solicita un nuevo código.");
        }

        // Validar el código
        if (!prc.getCodigo().equals(request.getCodigo())) {
            prc.setIntentos(prc.getIntentos() + 1);
            passwordResetCodeRepository.save(prc);
            
            int intentosRestantes = MAX_INTENTOS - prc.getIntentos();
            log.warn("⚠️ Código inválido para: {} - Intentos restantes: {}", email, intentosRestantes);
            
            if (intentosRestantes > 0) {
                throw new BusinessException("Código inválido. Te quedan " + intentosRestantes + " intento(s).");
            } else {
                throw new BusinessException("Código inválido. Se excedieron los intentos máximos. Solicita un nuevo código.");
            }
        }
        
        // Código válido - marcar como verificado
        prc.setVerificado(true);
        passwordResetCodeRepository.save(prc);
        
        log.info("✅ Código validado exitosamente para: {}", email);
    }

    /**
     * Restablece la contraseña del usuario con un código previamente validado.
     * Verifica que el código esté validado, no haya sido usado y no haya expirado.
     * 
     * @param request Contiene email, nueva contraseña y confirmación
     * @throws BusinessException si el código no está validado, fue usado o expiró
     * @throws ResourceNotFoundException si el usuario no existe
     */
    @Transactional
    public void resetearContrasenaPorId(ResetPasswordByIdRequestDTO request) {
        String email = request.getEmail().toLowerCase();
        log.info("🔐 Restableciendo contraseña para: {}", email);

        // Validar que las contraseñas coincidan
        if (!request.getContrasena().equals(request.getContrasenaConfirmacion())) {
            log.warn("⚠️ Las contraseñas no coinciden para: {}", email);
            throw new BusinessException("La contraseña y su confirmación no coinciden");
        }

        // Buscar usuario
        Persona persona = personasRepositorio.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("⚠️ Usuario no encontrado: {}", email);
                    return new ResourceNotFoundException("Usuario no encontrado");
                });

        // Buscar código de recuperación
        var opt = passwordResetCodeRepository.findTopByEmailOrderByIdDesc(email);
        PasswordResetCode prc = opt.orElseThrow(() -> {
            log.warn("⚠️ No hay código de recuperación para: {}", email);
            return new BusinessException("No hay una solicitud de recuperación para este correo");
        });

        // Validaciones del código
        if (!prc.isVerificado()) {
            log.warn("⚠️ Código no verificado para: {}", email);
            throw new BusinessException("Debes validar el código antes de restablecer la contraseña");
        }
        
        if (prc.isUsado()) {
            log.warn("⚠️ Código ya usado para: {}", email);
            throw new BusinessException("Este código ya fue utilizado. Por favor, solicita uno nuevo.");
        }
        
        if (Instant.now().isAfter(prc.getExpiraEn())) {
            log.warn("⚠️ Código expirado para: {}", email);
            throw new BusinessException("El código ha expirado. Por favor, solicita uno nuevo.");
        }

        // Validar que la nueva contraseña sea diferente a la actual
        if (passwordEncoder.matches(request.getContrasena(), persona.getContrasena())) {
            log.warn("⚠️ Nueva contraseña igual a la actual para: {}", email);
            throw new BusinessException("La nueva contraseña debe ser diferente a la actual");
        }

        // Actualizar contraseña
        persona.setContrasena(passwordEncoder.encode(request.getContrasena()));
        persona.setFechaActualizacion(LocalDate.now());
        
        // Resetear intentos fallidos de login si existieran
        persona.setFailedAttempts(0);
        persona.setLockedUntil(null);
        
        personasRepositorio.save(persona);

        // Marcar código como usado
        prc.setUsado(true);
        passwordResetCodeRepository.save(prc);

        log.info("✅ Contraseña restablecida exitosamente para: {}", persona.getEmail());
        
        // Opcional: Enviar notificación de confirmación
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", persona.getNombres());
            
            NotificationRequest req = NotificationRequest.builder()
                    .personaId(persona.getIdPersona())
                    .email(persona.getEmail())
                    .nombre(persona.getNombres())
                    .notiTipo(TipoNotificacion.CAMBIO_CONTRASENA)
                    .plantilla(TipoPlantilla.CONFIRMACION_RECUPERACION_CONTRASENA)
                    .params(params)
                    .titulo("Contraseña actualizada")
                    .mensaje("Tu contraseña ha sido restablecida exitosamente.")
                    .sendEmail(true)
                    .sendInApp(true)
                    .build();
                    
            notificationManager.notifyAllChannels(req);
            log.info("📧 Notificación de confirmación enviada a: {}", email);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo enviar notificación de confirmación: {}", e.getMessage());
            // No fallar el proceso por esto
        }
    }

    /**
     * Verifica la cuenta de un usuario mediante token JWT.
     * El token debe ser del tipo 'email_verification' y contener el email del usuario.
     * Al verificar exitosamente, marca el campo 'verificado' como true.
     * 
     * @param token Token JWT de verificación
     * @throws BusinessException si el token es inválido, expirado o no es de verificación
     * @throws ResourceNotFoundException si el usuario no existe
     */
    @Transactional
    public void verificarCuenta(String token) {
        log.info("Iniciando verificación de cuenta con token");
        
        try {
            // 1. Validar token (firma y expiración)
            if (!jwtUtil.validateToken(token)) {
                log.warn("Token de verificación inválido o expirado");
                throw new BusinessException("El token de verificación es inválido o ha expirado");
            }
            
            // 2. Verificar que el claim "type" sea email_verification
            String type = jwtUtil.extractType(token);
            if (!"email_verification".equals(type)) {
                log.warn("Token no es de tipo verificación de email: {}", type);
                throw new BusinessException("El token no es válido para verificación de cuenta");
            }
            
            // 3. Extraer email del token
            String email = jwtUtil.extractUsername(token);
            log.info("Verificando cuenta para email: {}", email);
            
            // 4. Buscar usuario (puede ser Cliente o eventualmente Administrador)
            Persona persona = personasRepositorio.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            
            // 5. Verificar que sea un Cliente
            if (!(persona instanceof Cliente)) {
                log.warn("Intento de verificar cuenta para un usuario que no es Cliente: {}", email);
                throw new BusinessException("Solo los clientes pueden verificar su cuenta mediante este método");
            }
            
            Cliente cliente = (Cliente) persona;
            
            // 6. Verificar si ya está verificado
            if (Boolean.TRUE.equals(cliente.getVerificado())) {
                log.info("La cuenta ya estaba verificada: {}", email);
                // No lanzar error, simplemente retornar exitosamente
                return;
            }
            
            // 7. Marcar como verificado
            cliente.setVerificado(true);
            cliente.setFechaActualizacion(LocalDate.now());
            clienteRepository.save(cliente);
            
            log.info("✅ Cuenta verificada exitosamente para: {}", email);
            
            // 8. Opcional: Enviar notificación de bienvenida
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("nombre", cliente.getNombres());
                
                NotificationRequest req = NotificationRequest.builder()
                        .personaId(cliente.getIdPersona())
                        .email(cliente.getEmail())
                        .nombre(cliente.getNombres())
                        .notiTipo(TipoNotificacion.SISTEMA)
                        .titulo("Cuenta verificada")
                        .mensaje("¡Tu cuenta ha sido verificada exitosamente! Ahora puedes disfrutar de todos los beneficios de Fasticket.")
                        .sendEmail(false) // Solo notificación in-app
                        .sendInApp(true)
                        .build();
                        
                notificationManager.notifyAllChannels(req);
            } catch (Exception e) {
                log.warn("No se pudo enviar notificación de bienvenida: {}", e.getMessage());
                // No fallar el proceso por esto
            }
            
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token de verificación expirado");
            throw new BusinessException("El token de verificación ha expirado. Por favor, solicita un nuevo enlace de verificación");
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("Error al procesar token JWT: {}", e.getMessage());
            throw new BusinessException("Token de verificación inválido");
        } catch (BusinessException | ResourceNotFoundException e) {
            // Re-lanzar excepciones de negocio
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al verificar cuenta: {}", e.getMessage(), e);
            throw new BusinessException("Error al verificar la cuenta. Por favor, intenta nuevamente");
        }
    }

    /**
     * Reenvía el correo de verificación a un usuario registrado.
     * Valida que el usuario exista, sea un Cliente y no esté ya verificado.
     * 
     * @param email Email del usuario que solicita el reenvío
     * @throws BusinessException si el usuario no existe, no es Cliente, ya está verificado o no se puede enviar el correo
     */
    @Transactional
    public void reenviarCorreoVerificacion(String email) {
        log.info("Solicitud de reenvío de correo de verificación para: {}", email);
        
        // 1. Buscar usuario por email
        Persona persona = personasRepositorio.findByEmail(email.toLowerCase())
                .orElseThrow(() -> {
                    log.warn("Intento de reenvío de verificación para email no registrado: {}", email);
                    // Por seguridad, no revelamos si el email existe o no
                    throw new BusinessException("Si el email está registrado y no está verificado, se enviará un correo de verificación.");
                });
        
        // 2. Validar que sea un Cliente (los Administradores no requieren verificación)
        if (persona.getRol() != Rol.CLIENTE) {
            log.warn("Intento de reenvío de verificación para usuario que no es Cliente: {} (rol: {})", email, persona.getRol());
            throw new BusinessException("Este tipo de cuenta no requiere verificación por correo electrónico.");
        }
        
        // 3. Cargar el Cliente completo
        Cliente cliente = clienteRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> {
                    log.error("Error: Cliente no encontrado aunque Persona existe: {}", email);
                    throw new BusinessException("Error al cargar información del cliente");
                });
        
        // 4. Validar que no esté ya verificado
        if (Boolean.TRUE.equals(cliente.getVerificado())) {
            log.info("Intento de reenvío de verificación para cuenta ya verificada: {}", email);
            // No lanzar error, simplemente informar que ya está verificado
            throw new BusinessException("Tu cuenta ya está verificada. Puedes iniciar sesión normalmente.");
        }
        
        // 5. Validar que la cuenta esté activa
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            log.warn("Intento de reenvío de verificación para cuenta desactivada: {}", email);
            throw new BusinessException("Tu cuenta está desactivada. Contacta con soporte para más información.");
        }
        
        // 6. Generar nuevo token de verificación
        try {
            log.info("📧 Generando nuevo token de verificación para: {}", email);
            String tokenVerificacion = jwtUtil.generateVerificationToken(cliente.getEmail());
            
            // Construir URL completa: FRONTEND_URL + /verificar-cuenta/ + token
            String linkVerificacion = frontendUrl.endsWith("/") 
                ? frontendUrl + "verificar-cuenta/" + tokenVerificacion
                : frontendUrl + "/verificar-cuenta/" + tokenVerificacion;
            
            log.info("🔗 Nuevo link de verificación generado para: {}", email);
            log.debug("Link de verificación (oculto en producción): {}***", 
                    linkVerificacion.substring(0, Math.min(50, linkVerificacion.length())));

            // 7. Preparar parámetros para la plantilla
            Map<String, Object> params = new HashMap<>();
            params.put("nombre", cliente.getNombres());
            params.put("linkVerificacion", linkVerificacion);
            params.put("email", cliente.getEmail());

            // 8. Enviar correo de verificación
            NotificationRequest req = NotificationRequest.builder()
                    .personaId(cliente.getIdPersona())
                    .email(cliente.getEmail())
                    .nombre(cliente.getNombres())
                    .notiTipo(TipoNotificacion.VERIFICACION_CUENTA)
                    .plantilla(TipoPlantilla.VERIFICAR_CUENTA)
                    .params(params)
                    .titulo("Verifica tu cuenta")
                    .mensaje("Hemos enviado un nuevo correo con el enlace de verificación.")
                    .sendEmail(true)
                    .sendInApp(false) // No enviar notificación in-app para reenvíos
                    .build();
                    
            notificationManager.notifyAllChannels(req);
            log.info("✅ Correo de verificación reenviado exitosamente a: {}", email);
            
        } catch (Exception e) {
            log.error("❌ Error reenviando correo de verificación a {}: {}", email, e.getMessage(), e);
            throw new BusinessException("No se pudo enviar el correo de verificación. Por favor, intenta nuevamente más tarde.");
        }
    }

}

