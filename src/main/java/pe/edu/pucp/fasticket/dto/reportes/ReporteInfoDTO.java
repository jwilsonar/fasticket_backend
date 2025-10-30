package pe.edu.pucp.fasticket.dto.reportes;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReporteInfoDTO {
    private LocalDateTime fechaGeneracion;
    private String periodoCubierto;
}