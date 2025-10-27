package pe.edu.pucp.fasticket.controllers.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.auth.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.auth.AuthService;

@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios")
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
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
    public ResponseEntity<StandardResponse<String>> verificarToken(@AuthenticationPrincipal UserDetails userDetails) {
        String message = "Token válido para: " + userDetails.getUsername();
        StandardResponse<String> response = StandardResponse.success("Token verificado exitosamente", message);
        return ResponseEntity.ok(response);
    }
}

