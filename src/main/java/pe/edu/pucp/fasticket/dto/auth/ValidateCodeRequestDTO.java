package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ValidateCodeRequestDTO {
	@Schema(description = "Email del usuario", example = "usuario@example.com")
	@NotBlank
	@Email
	private String email;

	@Schema(description = "Código de 6 dígitos enviado al email", example = "123456")
	@NotBlank
	@Pattern(regexp = "^[0-9]{6}$", message = "El código debe tener 6 dígitos")
	private String codigo;
}


