package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar el reenvío del correo de verificación de cuenta.
 */
@Schema(description = "Datos de solicitud para reenviar correo de verificación")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReenviarVerificacionRequestDTO {
    
    @Schema(
        description = "Email del usuario registrado que necesita verificación", 
        example = "usuario@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;
}

