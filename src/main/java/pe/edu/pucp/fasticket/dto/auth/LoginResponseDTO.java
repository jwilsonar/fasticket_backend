package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Respuesta de login exitoso")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    
    @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "Tipo de token", example = "Bearer")
    private String tipo;
    
    @Schema(description = "ID del usuario", example = "1")
    private Integer idUsuario;
    
    @Schema(description = "Email del usuario", example = "usuario@example.com")
    private String email;
    
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String nombreCompleto;
    
    @Schema(description = "Rol del usuario", example = "CLIENTE")
    private String rol;
    
    @Schema(description = "Tiempo de expiración del token en milisegundos", example = "86400000")
    private Long expiracion;
}

