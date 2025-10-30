package pe.edu.pucp.fasticket.dto.fidelizacion;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoCodigoPromocional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoPromocionalRequestDTO {
    @NotBlank(message = "El código es obligatorio")
    private String codigo;
    
    private String descripcion;
    
    private LocalDateTime fechaFin;
    
    @NotNull(message = "El tipo es obligatorio")
    private TipoCodigoPromocional tipo;
    
    @NotNull(message = "El valor es obligatorio")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0")
    private Double valor;
    
    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
    private Integer stock;
    
    private Integer cantidadPorCliente;
}

