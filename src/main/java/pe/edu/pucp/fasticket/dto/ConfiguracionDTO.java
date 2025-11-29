package pe.edu.pucp.fasticket.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class ConfiguracionDTO {

    @NotBlank
    @Size(max = 50)
    private String key;

    // valor como texto (se usa para enviar/recibir desde API)
    private String value;

    // descripción opcional para el admin
    private String descripcion;

    // tipo opcional: STRING, INTEGER, DOUBLE, MINUTES
    private String valueType;
}