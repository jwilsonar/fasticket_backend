package pe.edu.pucp.fasticket.dto.soporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;

@Data
@Schema(description = "Payload para actualizar el estado operativo del ticket")
public class ActualizarEstadoSolicitudDTO {

    @NotNull
    @Schema(description = "Estado destino", example = "RESUELTO")
    private EstadoSoporte estado;

    @Schema(description = "Comentarios sobre el cambio de estado")
    private String observaciones;
}

