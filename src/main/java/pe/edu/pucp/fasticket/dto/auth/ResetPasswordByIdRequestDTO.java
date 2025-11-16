package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResetPasswordByIdRequestDTO {
	@NotNull
	@Schema(description = "ID del cliente (persona)", example = "123")
	private Integer idCliente;

	@NotBlank
	@Schema(description = "Nueva contraseña", example = "S3gura!")
	private String contrasena;

	@NotBlank
	@Schema(description = "Confirmación de la nueva contraseña", example = "S3gura!")
	private String contrasenaConfirmacion;
}


