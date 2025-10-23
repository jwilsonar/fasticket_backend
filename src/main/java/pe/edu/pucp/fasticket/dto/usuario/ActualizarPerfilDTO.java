package pe.edu.pucp.fasticket.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar el perfil del cliente.
 * RF-060: Permite actualizar nombres, apellidos, teléfono y dirección.
 */
@Schema(description = "Datos para actualizar el perfil del cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPerfilDTO {
    
    @Schema(description = "Nombres del cliente", example = "Juan Carlos")
    @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
    private String nombres;
    
    @Schema(description = "Apellidos del cliente", example = "García López")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;
    
    @Schema(description = "Teléfono del cliente", example = "+51987654321")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
    private String telefono;
    
    @Schema(description = "Dirección del cliente", example = "Av. Principal 123, Lima")
    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccion;
    
    @Schema(description = "Email del cliente (opcional)", example = "nuevo.email@example.com")
    @Email(message = "El email debe ser válido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    private String email;
}

