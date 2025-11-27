package pe.edu.pucp.fasticket.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromoverUsuarioDTO {
    @NotBlank(message = "El cargo es obligatorio")
    private String cargo; // Ej: "Gerente de Marketing", "Soporte"
}