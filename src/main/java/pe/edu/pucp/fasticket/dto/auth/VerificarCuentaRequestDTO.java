package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para verificar la cuenta de un usuario mediante token JWT.
 */
@Schema(description = "Datos de solicitud para verificar cuenta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificarCuentaRequestDTO {
    
    @Schema(
        description = "Token JWT de verificación enviado por correo", 
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "El token de verificación es obligatorio")
    private String token;
}

