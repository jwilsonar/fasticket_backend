package pe.edu.pucp.fasticket.dto.soporte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.edu.pucp.fasticket.model.soporte.PrioridadSoporte;

@Data
@Schema(description = "Payload para registrar un nuevo ticket de soporte")
public class CrearSolicitudSoporteDTO {

    @Schema(description = "Identificador del usuario autenticado, opcional")
    private Integer idUsuario;

    @NotBlank
    @Size(max = 150)
    @Schema(description = "Asunto breve del ticket", example = "Problemas con el inicio de sesión")
    private String asunto;

    @NotBlank
    @Schema(description = "Descripción detallada del incidente")
    private String mensaje;

    @Schema(description = "Prioridad del ticket", defaultValue = "MEDIA")
    private PrioridadSoporte prioridad = PrioridadSoporte.MEDIA;

    @Size(max = 50)
    @Schema(description = "Canal donde se originó la solicitud", example = "PORTAL_WEB")
    private String canalOrigen;

    @Size(max = 45)
    @Schema(description = "Dirección IP capturada al momento de registrar la incidencia")
    private String ipOrigen;

    @Schema(description = "Metadatos técnicos adicionales", nullable = true)
    private String metadataAdicional;
}

