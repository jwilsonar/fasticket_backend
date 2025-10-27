package pe.edu.pucp.fasticket.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;

import java.time.LocalDate;

/**
 * Mapper para convertir entre entidades Evento y DTOs.
 */
@Component
@RequiredArgsConstructor
public class EventoMapper {

    public EventoResponseDTO toResponseDTO(Evento evento) {
        if (evento == null) {
            return null;
        }
        
        return EventoResponseDTO.builder()
                .idEvento(evento.getIdEvento())
                .nombre(evento.getNombre())
                .descripcion(evento.getDescripcion())
                .fechaEvento(evento.getFechaEvento())
                .horaInicio(evento.getHoraInicio())
                .horaFin(evento.getHoraFin())
                .imagenUrl(evento.getImagenUrl())
                .tipoEvento(evento.getTipoEvento())
                .estadoEvento(evento.getEstadoEvento())
                .aforoDisponible(evento.getAforoDisponible())
                .activo(evento.getActivo())
                .idLocal(evento.getLocal() != null ? evento.getLocal().getIdLocal() : null)
                .nombreLocal(evento.getLocal() != null ? evento.getLocal().getNombre() : null)
                .fechaCreacion(evento.getFechaCreacion())
                .build();
    }

    public Evento toEntity(EventoCreateDTO dto, Local local) {
        Evento evento = new Evento();
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFechaEvento(dto.getFechaEvento());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setEstadoEvento(dto.getEstadoEvento() != null ? dto.getEstadoEvento() : EstadoEvento.ACTIVO);
        evento.setAforoDisponible(dto.getAforoDisponible());
        evento.setLocal(local);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());
        return evento;
    }

    public void updateEntity(Evento evento, EventoCreateDTO dto, Local local) {
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFechaEvento(dto.getFechaEvento());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setTipoEvento(dto.getTipoEvento());
        if (dto.getEstadoEvento() != null) {
            evento.setEstadoEvento(dto.getEstadoEvento());
        }
        evento.setAforoDisponible(dto.getAforoDisponible());
        if (local != null) {
            evento.setLocal(local);
        }
        evento.setFechaActualizacion(LocalDate.now());
    }
}

