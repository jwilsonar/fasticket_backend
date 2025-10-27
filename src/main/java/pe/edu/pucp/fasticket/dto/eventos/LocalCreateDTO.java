package pe.edu.pucp.fasticket.dto.eventos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Datos para crear un nuevo local")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalCreateDTO {
    
    @Schema(description = "Nombre del local", example = "Estadio Nacional", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    private String nombre;

    @Schema(description = "Dirección completa del local", example = "Av. José Díaz, Lima")
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    private String direccion;

    @Schema(description = "Capacidad máxima del local", example = "45000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El aforo total es obligatorio")
    @Positive(message = "El aforo debe ser positivo")
    private Integer aforoTotal;

    @Schema(description = "ID del distrito", example = "1")
    private Integer idDistrito;
}

