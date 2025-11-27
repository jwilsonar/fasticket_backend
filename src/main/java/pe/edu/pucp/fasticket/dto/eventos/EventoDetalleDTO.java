package pe.edu.pucp.fasticket.dto.eventos;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import pe.edu.pucp.fasticket.model.eventos.Evento;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventoDetalleDTO {
    private Integer id;
    private String nombre;
    private LocalDate fecha;
    private LocalDate fechaFinEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean menoresDeEdadPermitidos;
    private String restricciones;
    private String politicasDevolucion;
    private String imagenUrl;
    private String imagenZonasUrl;
    private String descripcion;
    private LocalDetalleDTO local;
    private List<TipoTicketCompraDTO> tiposDeTicket;
}
