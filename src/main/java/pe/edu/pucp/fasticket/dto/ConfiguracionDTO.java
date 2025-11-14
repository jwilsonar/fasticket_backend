package pe.edu.pucp.fasticket.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class ConfiguracionDTO {

    @NotBlank
    @Size(max = 50)
    private String key;

    @NotBlank
    private String value;

    private String descripcion;
}