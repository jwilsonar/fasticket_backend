package pe.edu.pucp.fasticket.dto.soporte;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;
import pe.edu.pucp.fasticket.model.soporte.PrioridadSoporte;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representa una solicitud creada en el módulo de soporte")
public class SolicitudSoporteResponseDTO {

    @Schema(description = "Identificador interno de la solicitud")
    private Long idSolicitud;

    @Schema(description = "Identificador del usuario asociado (si existiera)", nullable = true)
    private Integer idUsuario;

    @Schema(description = "Nombre del usuario asociado", nullable = true)
    private String nombreUsuario;

    @Schema(description = "Correo electrónico del usuario asociado", nullable = true)
    private String emailUsuario;

    @Schema(description = "Asunto resumido del ticket")
    private String asunto;

    @Schema(description = "Descripción detallada del problema")
    private String mensaje;

    @Schema(description = "Estado actual del ticket")
    private EstadoSoporte estado;

    @Schema(description = "Prioridad operativa asignada")
    private PrioridadSoporte prioridad;

    @Schema(description = "Canal desde el que se originó la solicitud", nullable = true)
    private String canalOrigen;

    @Schema(description = "Dirección IP capturada al momento de crear la solicitud", nullable = true)
    private String ipOrigen;

    @Schema(description = "Metadatos técnicos adicionales", nullable = true)
    private String metadataAdicional;

    @Schema(description = "Comentarios administrativos", nullable = true)
    private String observaciones;

    @Schema(description = "Fecha de creación del ticket")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Última fecha de actualización")
    private LocalDateTime fechaActualizacion;

    @Schema(description = "Fecha de cierre/resolución del ticket", nullable = true)
    private LocalDateTime fechaCierre;
}

