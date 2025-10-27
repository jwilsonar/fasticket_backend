package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Datos para cambio de contraseña")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioContrasenaDTO {
    
    @Schema(description = "Contraseña actual", example = "OldPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String contrasenaActual;
    
    @Schema(description = "Nueva contraseña", example = "NewPassword456!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String contrasenaNueva;
    
    @Schema(description = "Confirmación de nueva contraseña", example = "NewPassword456!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String contrasenaConfirmacion;
}

