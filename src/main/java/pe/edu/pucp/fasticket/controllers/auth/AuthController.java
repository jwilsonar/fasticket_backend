package pe.edu.pucp.fasticket.controllers.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.auth.CambioContrasenaDTO;
import pe.edu.pucp.fasticket.dto.auth.ForgotPasswordRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.LoginRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.LoginResponseDTO;
import pe.edu.pucp.fasticket.dto.auth.ReenviarVerificacionRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.RegistroRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.ValidateCodeRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.VerificarCuentaRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.auth.AuthService;

@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PersonasRepositorio personasRepositorio;

    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica un usuario y devuelve un token JWT válido por 24 horas. " +
                     "Los clientes deben tener su cuenta verificada por correo electrónico antes de poder iniciar sesión. " +
                     "Los administradores no requieren verificación de cuenta."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cuenta no verificada (solo clientes) o cuenta bloqueada/desactivada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/login")
    public ResponseEntity<StandardResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("POST /api/v1/auth/login - Email: {}", request.getEmail());
        LoginResponseDTO response = authService.login(request);
        StandardResponse<LoginResponseDTO> standardResponse = StandardResponse.success("Login exitoso", response);
        return ResponseEntity.ok(standardResponse);
    }

    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea una cuenta de cliente. Se envía un correo de verificación al email proporcionado. " +
                     "El usuario debe verificar su correo haciendo clic en el enlace enviado. " +
                     "Devuelve un token JWT automáticamente para poder usar la aplicación."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Usuario registrado exitosamente. Se ha enviado un correo de verificación.",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o incompletos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email o documento ya registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/registro")
    public ResponseEntity<StandardResponse<LoginResponseDTO>> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        log.info("POST /api/v1/auth/registro - Email: {}", request.getEmail());
        LoginResponseDTO response = authService.registrarCliente(request);
        
        String mensaje = "Registro exitoso. Se ha enviado un correo de verificación a " + request.getEmail() + 
                        ". Por favor, verifica tu cuenta para disfrutar de todos los beneficios.";
            
        StandardResponse<LoginResponseDTO> standardResponse = StandardResponse.success(mensaje, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(standardResponse);
    }

    @Operation(
        summary = "Cambiar contraseña",
        description = "Permite a un usuario autenticado cambiar su contraseña"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Contraseña cambiada exitosamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Contraseña actual incorrecta o datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        )
    })
    @PutMapping("/cambiar-contrasena")
    public ResponseEntity<StandardResponse<String>> cambiarContrasena(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CambioContrasenaDTO request) {
        
        log.info("PUT /api/v1/auth/cambiar-contrasena - Usuario: {}", userDetails.getUsername());
        
        Persona persona = personasRepositorio.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        authService.cambiarContrasena(persona.getIdPersona(), request);
        
        StandardResponse<String> response = StandardResponse.success("Contraseña cambiada exitosamente");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Verificar token",
        description = "Valida si el token JWT actual es válido"
    )
    @ApiResponse(responseCode = "200", description = "Token válido")
    @GetMapping("/verificar")
    public ResponseEntity<pe.edu.pucp.fasticket.dto.StandardResponse<String>> verificarToken(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/auth/verificar - Usuario: {}", userDetails.getUsername());
        return ResponseEntity.ok(pe.edu.pucp.fasticket.dto.StandardResponse.success("Token válido", userDetails.getUsername()));
    }

    @Operation(
        summary = "Cerrar sesión",
        description = "Invalida el token JWT actual para cerrar la sesión del usuario"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Sesión cerrada exitosamente"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<StandardResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String email) {
        
        log.info("POST /api/v1/auth/logout - Email: {}", email);
        
        authService.logout(authHeader);
        
        StandardResponse<Void> response = StandardResponse.success("Sesión cerrada exitosamente", null);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = "Inicia el proceso de recuperación de contraseña enviando un código de 6 dígitos al correo electrónico. " +
                     "Por seguridad, siempre responde con éxito independientemente de si el email existe. " +
                     "El código expira en 10 minutos y debe ser validado antes de resetear la contraseña."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Solicitud procesada. Si el email existe, se envió un código de verificación."
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error al enviar el correo de recuperación",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/olvido-contrasena")
    public ResponseEntity<StandardResponse<String>> olvidoContrasena(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        log.info("PUT /api/v1/auth/olvido-contrasena - Correo: {}", request.getEmail());

        // Validar que el email no esté vacío
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            log.warn("⚠️ Intento de olvido de contraseña con email vacío");
            StandardResponse<String> response = StandardResponse.success(
                "Si el correo existe en nuestro sistema, recibirás un código de verificación."
            );
            return ResponseEntity.ok(response);
        }

        try {
            // Siempre intentar enviar el correo, sin importar si el usuario existe o no
            log.info("📧 Procediendo con envío de correo de recuperación");
            authService.iniciarOlvidoContrasena(request.getEmail());
            log.info("✅ Proceso de olvido de contraseña completado exitosamente");
            
        } catch (BusinessException e) {
            log.error("❌ Error de negocio al procesar olvido de contraseña: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado al procesar olvido de contraseña: {}", e.getMessage(), e);
            throw e;
        }

        StandardResponse<String> response = StandardResponse.success(
            "Si el correo existe en nuestro sistema, recibirás un código de verificación. Por favor, revisa tu bandeja de entrada."
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Validar código de recuperación de contraseña",
        description = "Valida el código de 6 dígitos enviado al correo electrónico. " +
                     "El código debe ser validado antes de poder resetear la contraseña. " +
                     "El código expira en 10 minutos desde su generación."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Código validado exitosamente. Ahora puede proceder a resetear la contraseña."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Código inválido, expirado o ya usado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No hay solicitud de recuperación vigente para este email",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/olvido-contrasena/validar")
    public ResponseEntity<StandardResponse<String>> validarCodigo(@Valid @RequestBody ValidateCodeRequestDTO request) {
        log.info("POST /api/v1/auth/olvido-contrasena/validar - Email: {}", request.getEmail());
        authService.validarCodigoOlvido(request);
        return ResponseEntity.ok(StandardResponse.success("Código validado exitosamente. Ahora puedes restablecer tu contraseña."));
    }

    @Operation(
        summary = "Restablecer contraseña con código validado",
        description = "Restablece la contraseña del usuario después de haber validado exitosamente el código de verificación. " +
                     "El código debe estar validado y no haber sido usado previamente. " +
                     "La nueva contraseña debe cumplir con los requisitos de seguridad y coincidir con su confirmación."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Contraseña actualizada exitosamente. Ya puedes iniciar sesión con tu nueva contraseña."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Código no validado, ya usado, expirado, o las contraseñas no coinciden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado o no hay código validado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/olvido-contrasena/reset")
    public ResponseEntity<StandardResponse<String>> resetPorEmail(
            @Valid @RequestBody pe.edu.pucp.fasticket.dto.auth.ResetPasswordByIdRequestDTO request) {
        
        log.info("PUT /api/v1/auth/olvido-contrasena/reset - Correo: {}", request.getEmail());
        authService.resetearContrasenaPorId(request);
        return ResponseEntity.ok(StandardResponse.success("Contraseña actualizada exitosamente. Ya puedes iniciar sesión con tu nueva contraseña."));
    }

    @Operation(
        summary = "Verificar cuenta de usuario",
        description = "Verifica la cuenta de un usuario mediante el token JWT enviado por correo electrónico. " +
                     "El token contiene el email del usuario y expira en 24 horas. " +
                     "Una vez verificada la cuenta, el usuario puede acceder a funcionalidades exclusivas."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta verificada exitosamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Token inválido, expirado o no es de verificación de cuenta",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/verificar/cuenta")
    public ResponseEntity<StandardResponse<Void>> verificarCuenta(@Valid @RequestBody VerificarCuentaRequestDTO request) {
        log.info("POST /api/v1/auth/verificar/cuenta - Procesando verificación de cuenta");
        authService.verificarCuenta(request.getToken());
        return ResponseEntity.ok(StandardResponse.success("¡Cuenta verificada exitosamente! Ahora puedes disfrutar de todos los beneficios de Fasticket."));
    }

    @Operation(
        summary = "Reenviar correo de verificación",
        description = "Reenvía el correo de verificación a un usuario registrado que aún no ha verificado su cuenta. " +
                     "Solo funciona para clientes que no han verificado su correo electrónico. " +
                     "Si el email está registrado y cumple las condiciones, se enviará un nuevo correo con el enlace de verificación."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Correo de verificación reenviado exitosamente (si el email existe y cumple las condiciones)"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Email inválido, cuenta ya verificada, cuenta desactivada, o no es un cliente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/verificar/reenviar")
    public ResponseEntity<StandardResponse<String>> reenviarCorreoVerificacion(@Valid @RequestBody ReenviarVerificacionRequestDTO request) {
        log.info("POST /api/v1/auth/verificar/reenviar - Email: {}", request.getEmail());
        
        try {
            authService.reenviarCorreoVerificacion(request.getEmail());
            return ResponseEntity.ok(StandardResponse.success(
                "Si el email está registrado y no está verificado, se ha enviado un nuevo correo de verificación. " +
                "Por favor, revisa tu bandeja de entrada y carpeta de spam."
            ));
        } catch (BusinessException e) {
            // Si es una excepción de negocio (cuenta ya verificada, etc.), retornar el mensaje específico
            return ResponseEntity.ok(StandardResponse.success(e.getMessage()));
        }
    }

}

