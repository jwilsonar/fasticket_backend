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
        
        // **CORRECCIÓN: Se añaden todos los campos faltantes (activo, tipoEvento, local, etc.)**
        return EventoResponseDTO.builder()
                .idEvento(evento.getIdEvento())
                .nombre(evento.getNombre())
                .descripcion(evento.getDescripcion())
                .fechaEvento(evento.getFechaEvento())
                .fechaFinEvento(evento.getFechaFinEvento())
                .horaInicio(evento.getHoraInicio())
                .horaFin(evento.getHoraFin())
                .imagenUrl(evento.getImagenUrl()) 
                .imagenZonasUrl(evento.getImagenZonasUrl()) 
                
                // --- Campos previamente omitidos que causaban fallos ---
                .tipoEvento(evento.getTipoEvento()) // Causa: Falla de TipoEvento (ROCK, POP)
                .estadoEvento(evento.getEstadoEvento())
                .aforoDisponible(evento.getAforoDisponible())
                .menoresDeEdadPermitidos(evento.getMenoresDeEdadPermitidos()) // Causa: Falla de menoresDeEdadPermitidos
                .restricciones(evento.getRestricciones())
                .politicasDevolucion(evento.getPoliticasDevolucion())
                
                // --- Campos de Estado y Auditoría que causaban NPE ---
                .activo(evento.getActivo()) // Causa: NullPointer en getActivo()
                .fechaCreacion(evento.getFechaCreacion())


                
                // --- Mapeo de la información del Local ---
                .idLocal(evento.getLocal() != null ? evento.getLocal().getIdLocal() : null)
                .nombreLocal(evento.getLocal() != null ? evento.getLocal().getNombre() : null)
                
                .build();
    }

    public Evento toEntity(EventoCreateDTO dto, Local local) {
        Evento evento = new Evento();
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFechaEvento(dto.getFechaEvento());
        evento.setFechaFinEvento(dto.getFechaFinEvento());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setEstadoEvento(dto.getEstadoEvento() != null ? dto.getEstadoEvento() : EstadoEvento.ACTIVO);
        evento.setAforoDisponible(dto.getAforoDisponible());
        evento.setMenoresDeEdadPermitidos(dto.getMenoresDeEdadPermitidos());
        evento.setRestricciones(dto.getRestricciones());
        evento.setPoliticasDevolucion(dto.getPoliticasDevolucion());
        evento.setLocal(local);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());
        return evento;
    }

    public void updateEntity(Evento evento, EventoCreateDTO dto, Local local) {
        
        if (dto.getNombre() != null) evento.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) evento.setDescripcion(dto.getDescripcion());
        if (dto.getFechaEvento() != null) evento.setFechaEvento(dto.getFechaEvento());
        if (dto.getFechaFinEvento() != null) evento.setFechaFinEvento(dto.getFechaFinEvento());
        if (dto.getHoraInicio() != null) evento.setHoraInicio(dto.getHoraInicio());
        if (dto.getHoraFin() != null) evento.setHoraFin(dto.getHoraFin());
        if (dto.getMenoresDeEdadPermitidos() != null) evento.setMenoresDeEdadPermitidos(dto.getMenoresDeEdadPermitidos());
        if (dto.getRestricciones() != null) evento.setRestricciones(dto.getRestricciones());
        if (dto.getPoliticasDevolucion() != null) evento.setPoliticasDevolucion(dto.getPoliticasDevolucion());
        if (dto.getTipoEvento() != null) evento.setTipoEvento(dto.getTipoEvento());
        if (dto.getEstadoEvento() != null) {
            evento.setEstadoEvento(dto.getEstadoEvento());
        }
        if (dto.getAforoDisponible() != null) evento.setAforoDisponible(dto.getAforoDisponible());
        
        if (local != null) { 
            evento.setLocal(local);
        }
        evento.setFechaActualizacion(LocalDate.now());
    }
}