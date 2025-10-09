package pe.edu.pucp.fasticket.dto.eventos;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    private List<CategoriaEntrada> categoriasEntrada;

    public EventoDTO(Evento p_evento){
        this.idEvento = p_evento.getIdEvento();
    }
}
