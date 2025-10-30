package pe.edu.pucp.fasticket.dto.fidelizacion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoRegla;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReglaPuntosRequestDTO {
    @NotNull(message = "El valor de soles por punto es obligatorio")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0")
    private Double solesPorPunto;
    
    @NotNull(message = "El tipo de regla es obligatorio")
    private TipoRegla tipoRegla;
    
    private Boolean activo = true;
    private Boolean estado = true;
}

