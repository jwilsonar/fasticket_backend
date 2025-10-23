package pe.edu.pucp.fasticket.dto.tickets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TicketCreateDTO {

    @NotBlank(message = "El nombre de la entrada es obligatorio")
    private String nombre; // Ej: "Entrada Regular", "Entrada VIP"

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private Double precio;

    @NotNull(message = "El stock es obligatorio")
    @Positive(message = "El stock debe ser positivo")
    private Integer stock;

    @NotNull(message = "Debe asociar la entrada a una zona")
    private Integer idZona; // La FK a la Zona que creamos en el paso 2
}