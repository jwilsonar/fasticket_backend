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
import pe.edu.pucp.fasticket.dto.auth.RegistroRequestDTO;
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
        description = "Autentica un usuario y devuelve un token JWT válido por 24 horas"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
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
        description = "Crea una cuenta de usuario (cliente o administrador) basado en el dominio del email. " +
                     "Los emails @pucp.edu.pe se registran como administradores, otros como clientes. " +
                     "Devuelve un token JWT automáticamente."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Usuario registrado exitosamente",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
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
        
        String mensaje = response.getRol().equals("ADMINISTRADOR") 
            ? "Administrador registrado exitosamente" 
            : "Cliente registrado exitosamente";
            
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
            summary = "Olvido contraseña",
            description = "Permite al usuario obtener un correo para recuperar su contraseña, si el correo existe."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Correo enviado."
                    //Creo en este caso no debería haber ApiResponses
            )
    })
    @PutMapping("/olvido-contrasena")
    public ResponseEntity<StandardResponse<String>> olvidoContrasena(@RequestBody ForgotPasswordRequestDTO request) {

        log.info("PUT /api/v1/auth/olvido-contrasena - Correo: {}", request.getEmail());

        // Validar que el email no esté vacío
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            log.warn("⚠️ Intento de olvido de contraseña con email vacío");
            StandardResponse<String> response = StandardResponse.success("Se envió un código de verificación al correo proporcionado");
            return ResponseEntity.ok(response);
        }

        try {
            // Siempre intentar enviar el correo, sin importar si el usuario existe o no
            log.info("📧 Procediendo con envío de correo de recuperación (sin validar existencia de usuario)");
            authService.iniciarOlvidoContrasena(request.getEmail());
            log.info("✅ Proceso de olvido de contraseña completado exitosamente");
            
        } catch (BusinessException e) {
            log.error("❌ Error de negocio al procesar olvido de contraseña: {}", e.getMessage());
            // Si hay un error de negocio (como fallo en envío), lo propagamos
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado al procesar olvido de contraseña: {}", e.getMessage(), e);
            // Para otros errores, también los propagamos para que se manejen apropiadamente
            throw e;
        }

        StandardResponse<String> response = StandardResponse.success("Se envió un código de verificación al correo proporcionado");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validar código de olvido de contraseña")
    @PostMapping("/olvido-contrasena/validar")
    public ResponseEntity<StandardResponse<String>> validarCodigo(@RequestBody pe.edu.pucp.fasticket.dto.auth.ValidateCodeRequestDTO request) {
        authService.validarCodigoOlvido(request);
        return ResponseEntity.ok(StandardResponse.success("Código validado"));
    }

    @Operation(
        summary = "Resetear contraseña por correo",
        description = "Permite al usuario restablecer su contraseña usando su correo electrónico después de validar el código de verificación"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Contraseña actualizada exitosamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o código no validado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/olvido-contrasena/reset")
    public ResponseEntity<StandardResponse<String>> resetPorEmail(
            @Valid @RequestBody pe.edu.pucp.fasticket.dto.auth.ResetPasswordByIdRequestDTO request) {
        
        log.info("PUT /api/v1/auth/olvido-contrasena/reset - Correo: {}", request.getEmail());
        authService.resetearContrasenaPorId(request);
        return ResponseEntity.ok(StandardResponse.success("Contraseña actualizada exitosamente"));
    }

    @Operation(
            summary = "Verificar el token al crear una cuenta",
            description = "Permite al usuario verificar el correo que uso al crear una cuenta"
    )
    @ApiResponses({

            @ApiResponse(
                    responseCode = "400",
                    description = "Token inválido o token caducado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Token no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/verificar/cuenta")
    public ResponseEntity<StandardResponse<Void>> verificar(UserDetails userDetails, String token) {

        log.info("POST /api/v1/auth/verificar/cuenta - token: {}", token);
        authService.verificarCuenta(userDetails,token);
        return ResponseEntity.ok(StandardResponse.success("Se verifico la cuenta del usuario"));
    }

}

