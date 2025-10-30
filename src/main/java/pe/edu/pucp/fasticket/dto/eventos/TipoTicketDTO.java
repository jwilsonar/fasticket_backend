package pe.edu.pucp.fasticket.dto.eventos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para TipoTicket")
public class TipoTicketDTO {

    @Schema(description = "ID del tipo de ticket", example = "1")
    private Integer idTipoTicket;
    
    @Schema(description = "Nombre del tipo de ticket", example = "VIP")
    private String nombre;
    
    @Schema(description = "Descripción del tipo de ticket", example = "Acceso VIP con beneficios exclusivos")
    private String descripcion;
    
    @Schema(description = "Precio del ticket", example = "150.00")
    private Double precio;
    
    @Schema(description = "Stock disponible", example = "100")
    private Integer stock;
    
    @Schema(description = "Estado activo", example = "true")
    private Boolean activo;
    
    @Schema(description = "ID de la zona", example = "1")
    private Integer idZona;
    
    @Schema(description = "Nombre de la zona", example = "Zona VIP")
    private String nombreZona;
    
    @Schema(description = "Límite de tickets por persona", example = "4")
    private Integer limitePorPersona;
}
