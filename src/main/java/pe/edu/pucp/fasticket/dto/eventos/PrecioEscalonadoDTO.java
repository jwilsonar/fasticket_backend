package pe.edu.pucp.fasticket.dto.eventos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.Etapa;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para PrecioEscalonado")
public class PrecioEscalonadoDTO {

    @Schema(description = "ID del precio escalonado", example = "1")
    private Integer idPrecio;

    @Schema(description = "Nombre del precio escalonado", example = "PREVENTA")
    private Etapa nombreEtapa;

    @Schema(description = "Fecha inicial de validez del precio escalonado", example = "2024-07-01")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha final de validez del precio escalonado", example = "2024-07-31")
    private LocalDate fechaFin;

    @Schema(description = "Booleano para activar/descativar el precio escalonado", example = "true")
    private Boolean activo = true;
}
