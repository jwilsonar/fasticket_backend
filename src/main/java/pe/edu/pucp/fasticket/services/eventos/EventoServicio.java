package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;

import java.util.List;
import java.util.Optional;

@Service
public class EventoServicio {
    @Autowired
    private EventosRepositorio repo_eventos;

    public List<Evento> ListarEventos(){
        return repo_eventos.findAll();
    }

    public Optional<Evento> BuscarID(Integer id){
        return repo_eventos.findById(id);
    }

    public Evento Guardar(Evento evento){
        return (Evento) repo_eventos.save(evento);
    }

    public void Eliminar(Integer id){
        repo_eventos.deleteById(id);
    }
}

package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.dto.eventos.EventoDetalleDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalDetalleDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventoServicio {

    private final EventosRepositorio eventosRepositorio;

    public EventoServicio(EventosRepositorio eventosRepositorio) {
        this.eventosRepositorio = eventosRepositorio;
    }

    public EventoDetalleDTO obtenerDetallePorId(Integer id) {
        Evento evento = eventosRepositorio.findById(id).orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));

        EventoDetalleDTO dto = new EventoDetalleDTO();
        dto.setId(evento.getIdEvento());
        dto.setNombre(evento.getNombre());
        dto.setDescripcion(evento.getDescripcion());
        dto.setFecha(evento.getFechaEvento());
        dto.setHora(evento.getHoraInicio());
        dto.setUrlImagen(evento.getImagenUrl());

        if (evento.getLocal() != null) {
            LocalDetalleDTO localDTO = new LocalDetalleDTO();
            localDTO.setNombre(evento.getLocal().getNombre());
            localDTO.setDireccion(evento.getLocal().getDireccion());
            //localDTO.setUrlMapa(evento.getLocal().getUrlMapa());
            dto.setLocal(localDTO);
        }

        List<TipoTicketDTO> ticketsDTO = evento.getTiposTicket().stream().map(tipo -> {
            TipoTicketDTO ticketDTO = new TipoTicketDTO();
            ticketDTO.setId(tipo.getIdTipoTicket());
            ticketDTO.setNombre(tipo.getNombre());
            ticketDTO.setPrecio(tipo.getPrecio());
            return ticketDTO;
        }).collect(Collectors.toList());
        dto.setTiposDeTicket(ticketsDTO);

        return dto;
    }
}
