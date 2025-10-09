package pe.edu.pucp.fasticket.dto.eventos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

@Data
@NoArgsConstructor
public class EventoDTO {
    private Integer idEvento, usuarioCreacion;
    private String titulo, descripcion;
    private LocalDate fecha;
    private LocalTime hora;
    private byte[] urlImagen;
    private Boolean activo;
    private EstadoEvento estado;
    private TipoEvento tipo;
    private Local local;
    private List<Ticket> tickets;
    private List<TipoTicket> tiposTicket;

    public EventoDTO(Evento p_evento){
        this.idEvento = p_evento.getIdEvento();
    }
}
