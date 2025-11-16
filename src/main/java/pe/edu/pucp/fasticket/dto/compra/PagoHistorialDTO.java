package pe.edu.pucp.fasticket.dto.compra;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.pago.EstadoPago;

@Schema(description = "DTO para información de pago en historial")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoHistorialDTO {
    
    @Schema(description = "ID del pago", example = "1")
    private Integer idPago;
    
    @Schema(description = "Monto del pago", example = "150.00")
    private Double monto;
    
    @Schema(description = "Estado del pago", example = "APROBADO")
    private EstadoPago estado;
    
    @Schema(description = "Fecha del pago", example = "2024-01-15")
    private LocalDate fechaPago;
    
    @Schema(description = "Método de pago", example = "Tarjeta (1234)")
    private String metodo;
}

