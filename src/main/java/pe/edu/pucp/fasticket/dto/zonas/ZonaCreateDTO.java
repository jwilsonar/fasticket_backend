package pe.edu.pucp.fasticket.dto.zonas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ZonaCreateDTO {

    @NotBlank(message = "El nombre de la zona es obligatorio")
    private String nombre; // Ej: "VIP", "General", "Platea"

    @NotNull(message = "El aforo es obligatorio")
    @Positive(message = "El aforo debe ser un número positivo")
    private Integer aforoMax; // Ej: 1000

    @NotNull(message = "El id del local es obligatorio")
    @Positive(message = "El id del local debe ser un número positivo")
    private Integer idLocal; // id del Local al que pertenece la zona

    private String imagenUrl; // URL de la imagen de la zona
}