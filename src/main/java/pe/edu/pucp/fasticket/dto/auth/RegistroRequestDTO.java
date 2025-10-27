package pe.edu.pucp.fasticket.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;

import java.time.LocalDate;

@Schema(description = "Datos de solicitud para registro de cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequestDTO {
    
    @Schema(description = "Tipo de documento", example = "DNI", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;
    
    @Schema(description = "Número de documento", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres")
    private String docIdentidad;
    
    @Schema(description = "Nombres del usuario", example = "Juan Carlos", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
    private String nombres;
    
    @Schema(description = "Apellidos del usuario", example = "Pérez García", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;
    
    @Schema(description = "Email del usuario", example = "juan.perez@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;
    
    @Schema(description = "Contraseña", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
    private String contrasena;
    
    @Schema(description = "Teléfono", example = "987654321")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;
    
    @Schema(description = "Fecha de nacimiento", example = "1990-01-15")
    private LocalDate fechaNacimiento;
    
    @Schema(description = "Dirección", example = "Av. Principal 123, Lima")
    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccion;
    
    @Schema(description = "ID del distrito", example = "1")
    private Integer idDistrito;
}

