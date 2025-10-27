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
    private Integer idEvento, usuarioCreacion, usuarioActualizacion;
    private String nombre, descripcion;
    private LocalDate fechaEvento, fechaCreacion, fechaActualizacion;
    private LocalTime horaInicio, horaFin;
    private String urlImagen;
    private Boolean activo;
    private EstadoEvento estado;
    private TipoEvento tipo;
    private Local local;
    private List<Ticket> tickets;
    private List<TipoTicket> tiposTicket;

    //NOTA: Se supone que por Evento se crean varias Zonas, pero no hay conexión o datos para hacerlo en el model
    //NOTA DE NOTA: No lo incluyo en este momento porq no se exactamente como incluirlo, xd

    public EventoDTO(Evento p_evento){
        this.idEvento = p_evento.getIdEvento();
        this.usuarioCreacion = p_evento.getUsuarioCreacion();
        this.usuarioActualizacion = p_evento.getUsuarioActualizacion();
        this.nombre = p_evento.getNombre();
        this.descripcion = p_evento.getDescripcion();
        this.fechaEvento = p_evento.getFechaEvento();
        this.fechaCreacion = p_evento.getFechaCreacion();
        this.fechaActualizacion = p_evento.getFechaActualizacion();
        this.horaInicio = p_evento.getHoraInicio();
        this.horaFin = p_evento.getHoraFin();
        this.urlImagen = p_evento.getImagenUrl();
        this.activo = p_evento.getActivo();
        this.estado = p_evento.getEstadoEvento();
        this.tipo = p_evento.getTipoEvento();
        //this.local = p_evento.getLocal(); //No existe variable para el local
        this.tickets = p_evento.getTickets();
        //this.tiposTicket = p_evento.getTiposTicket(); //La conexión tiene q pasar por Zonas Primero
    }
}
