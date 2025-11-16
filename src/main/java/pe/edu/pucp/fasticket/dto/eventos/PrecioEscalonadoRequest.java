package pe.edu.pucp.fasticket.dto.eventos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.Etapa;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear PrecioEscalonado")
public class PrecioEscalonadoRequest {

    @Schema(description = "ID del escalon", example = "1")
    private Integer idPrecio;

    @Schema(description = "Nombre del escalon", example = "REGULAR")
    private Etapa nombreEtapa;

    @Schema(description = "Fecha de inicio", example = "10-12-2025")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha de fin", example = "18-12-2025")
    private LocalDate fechaFin;

    @Schema(description = "Estado activo del escalon", example = "true")
    private Boolean activo = true;

}
