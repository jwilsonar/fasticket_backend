package pe.edu.pucp.fasticket.dto.soporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.edu.pucp.fasticket.model.soporte.PrioridadSoporte;

@Data
@Schema(description = "Payload para actualizar el contenido de un ticket de soporte")
public class ActualizarSolicitudSoporteDTO {

    @Size(max = 150)
    @Schema(description = "Asunto actualizado del ticket")
    private String asunto;

    @Schema(description = "Mensaje detallado actualizado")
    private String mensaje;

    @Schema(description = "Nueva prioridad asignada", nullable = true)
    private PrioridadSoporte prioridad;

    @Size(max = 50)
    @Schema(description = "Canal corregido", nullable = true)
    private String canalOrigen;

    @Size(max = 45)
    @Schema(description = "IP corregida", nullable = true)
    private String ipOrigen;

    @Schema(description = "Metadatos adicionales", nullable = true)
    private String metadataAdicional;

    @Schema(description = "Observaciones internas", nullable = true)
    private String observaciones;
}

