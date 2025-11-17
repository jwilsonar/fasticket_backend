package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordByIdRequestDTO {
	@NotBlank(message = "El email es obligatorio")
	@Email(message = "Debe ser un email válido")
	@Schema(description = "Email del usuario", example = "usuario@example.com")
	private String email;

	@NotBlank
	@Schema(description = "Nueva contraseña", example = "S3gura!")
	private String contrasena;

	@NotBlank
	@Schema(description = "Confirmación de la nueva contraseña", example = "S3gura!")
	private String contrasenaConfirmacion;
}


