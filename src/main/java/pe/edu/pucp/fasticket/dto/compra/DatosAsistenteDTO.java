package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;

@Data
@NoArgsConstructor
@Schema(description = "DTO para datos de asistente en una compra")
public class DatosAsistenteDTO {
    
    @Schema(description = "ID del ticket (opcional, se asigna automáticamente)", example = "1")
    @NotNull(message = "ID del Ticket es obligatorio")
    private Integer idTicket;
    
    @Schema(description = "Tipo de documento del asistente", example = "DNI", required = true)
    @NotNull
    private TipoDocumento tipoDocumento;
    
    @Schema(description = "Número de documento del asistente", example = "12345678", required = true)
    @NotBlank
    private String numeroDocumento;
    
    @Schema(description = "Nombres del asistente", example = "Juan Carlos", required = true)
    @NotBlank
    private String nombres;
    
    @Schema(description = "Apellidos del asistente", example = "Pérez García", required = true)
    @NotBlank
    private String apellidos;

    public DatosAsistenteDTO(TipoDocumento tipoDocumento, String numeroDocumento, String nombres, String apellidos) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }
}