package pe.edu.pucp.fasticket.dto.reportes;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class EventoDetallesDTO {
    private Integer idEvento;
    private String titulo;
    private LocalDate fechaEvento;
    private String localNombre;
    private Integer aforoTotal;
}