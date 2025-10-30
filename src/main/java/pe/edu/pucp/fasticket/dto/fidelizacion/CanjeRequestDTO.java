package pe.edu.pucp.fasticket.dto.fidelizacion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanjeRequestDTO {
    @NotNull(message = "El ID del cliente es obligatorio")
    private Integer idCliente;
    
    @NotNull(message = "El ID de la orden de compra es obligatorio")
    private Integer idOrdenCompra;
    
    @NotNull(message = "La cantidad de puntos a canjear es obligatoria")
    @Min(value = 1, message = "La cantidad de puntos debe ser mayor a 0")
    private Integer puntosCanje;
    
    @NotNull(message = "El monto a descontar es obligatorio")
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double montoDescuento;
}

