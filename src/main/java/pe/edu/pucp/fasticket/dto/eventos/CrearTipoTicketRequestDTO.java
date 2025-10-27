package pe.edu.pucp.fasticket.dto.eventos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CrearTipoTicketRequestDTO {
    @NotNull(message = "El ID del evento es obligatorio")
    private Integer idEvento;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a cero")
    private Double precio;

    @NotNull(message = "El stock es obligatorio")
    @Positive(message = "El stock debe ser mayor a cero")
    private Integer stock;

    @NotNull(message = "La fecha de inicio de venta es obligatoria")
    private LocalDateTime fechaInicioVenta;

    @NotNull(message = "La fecha de fin de venta es obligatoria")
    private LocalDateTime fechaFinVenta;
}
