package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO para información de ticket en historial")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketHistorialDTO {
    
    @Schema(description = "ID del ticket", example = "1")
    private Integer idTicket;
    
    @Schema(description = "Código QR del ticket", example = "QR-ABC123")
    private String codigoQr;
    
    @Schema(description = "Asiento del ticket", example = "A12")
    private String asiento;
    
    @Schema(description = "Fila del ticket", example = "5")
    private String fila;
    
    @Schema(description = "Estado del ticket", example = "VENDIDA")
    private String estado;
}

