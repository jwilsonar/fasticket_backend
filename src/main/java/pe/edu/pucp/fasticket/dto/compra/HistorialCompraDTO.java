package pe.edu.pucp.fasticket.dto.compra;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;

@Schema(description = "DTO para historial de compras del cliente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCompraDTO {
    
    @Schema(description = "ID de la orden de compra", example = "1")
    private Integer idOrdenCompra;
    
    @Schema(description = "Fecha de la orden", example = "2024-01-15")
    private LocalDate fechaOrden;
    
    @Schema(description = "Total de la orden", example = "150.00")
    private Double total;
    
    @Schema(description = "Estado de la compra", example = "VENDIDA")
    private EstadoCompra estado;
    
    @Schema(description = "Código de seguimiento", example = "ORD-001")
    private String codigoSeguimiento;
    
    @Schema(description = "Información del pago")
    private PagoHistorialDTO pago;
    
    @Schema(description = "Información del evento")
    private EventoHistorialDTO evento;
    
    @Schema(description = "Items de la orden")
    private List<ItemHistorialDTO> items;
    
    @Schema(description = "Tickets de la orden")
    private List<TicketHistorialDTO> tickets;
}

