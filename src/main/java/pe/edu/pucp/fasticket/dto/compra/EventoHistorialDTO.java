package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO para información del evento en historial")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoHistorialDTO {
    
    @Schema(description = "ID del evento", example = "1")
    private Integer idEvento;
    
    @Schema(description = "Nombre del evento", example = "Concierto de Rock")
    private String nombre;
    
    @Schema(description = "URL de la imagen del evento", example = "https://example.com/imagen.jpg")
    private String imagenUrl;
}

