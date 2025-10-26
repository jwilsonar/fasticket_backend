package pe.edu.pucp.fasticket.dto.eventos;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
public class EventoDetalleDTO {
    private Integer id;
    private String nombre;
    private LocalDate fecha;
    private LocalTime hora;
    private String urlImagen;
    private String descripcion;
    private LocalDetalleDTO local;
    private List<TipoTicketCompraDTO> tiposDeTicket;
}
