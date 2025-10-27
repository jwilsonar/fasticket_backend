package pe.edu.pucp.fasticket.dto.eventos;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

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
