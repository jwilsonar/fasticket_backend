package pe.edu.pucp.fasticket.dto.fidelizacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidacionCuponResponseDTO {
    private String codigo;
    private String tipo;
    private Double valorCupon;
}