package pe.edu.pucp.fasticket.dto.eventos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventoResumenDTO {
    private String nombreEvento;
    private LocalDate fecha;
    private LocalTime hora;
    private String nombreLocal;
}