package pe.edu.pucp.fasticket.dto.eventos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActualizarTipoTicketRequestDTO {
    @NotBlank
    private String nombre;
    @NotBlank
    private String descripcion;
    @NotNull @Positive
    private Double precio;
    @NotNull @Positive
    private Integer stock;
    @NotNull
    private LocalDateTime fechaInicioVenta;
    @NotNull
    private LocalDateTime fechaFinVenta;
}
