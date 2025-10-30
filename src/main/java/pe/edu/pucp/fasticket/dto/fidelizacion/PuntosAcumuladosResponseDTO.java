package pe.edu.pucp.fasticket.dto.fidelizacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntosAcumuladosResponseDTO {
    private Integer idCliente;
    private Integer puntosAcumulados;
    private String mensaje;
}

