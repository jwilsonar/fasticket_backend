package pe.edu.pucp.fasticket.mapper;

import lombok.RequiredArgsConstructor;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;
import pe.edu.pucp.fasticket.dto.eventos.*;
import pe.edu.pucp.fasticket.model.eventos.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir entre entidades Evento y DTOs.
 */
@Component
@RequiredArgsConstructor
@Mapper
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
                .imagenZonasUrl(evento.getImagenZonasUrl())
                .tipoEvento(evento.getTipoEvento())
                .estadoEvento(evento.getEstadoEvento())
                .aforoDisponible(evento.getAforoDisponible())
                .activo(evento.getActivo())
                .idLocal(evento.getLocal() != null ? evento.getLocal().getIdLocal() : null)
                .menoresDeEdadPermitidos(evento.getMenoresDeEdadPermitidos())
                .restricciones(evento.getRestricciones())
                .politicasDevolucion(evento.getPoliticasDevolucion())
                .nombreLocal(evento.getLocal() != null ? evento.getLocal().getNombre() : null)
                .fechaCreacion(evento.getFechaCreacion())
                .build();
    }

    @Mapping(target = "id", ignore = true)
    public Evento toEntity(EventoCreateDTO dto, Local local) {
        Evento evento = new Evento();
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFechaEvento(dto.getFechaEvento());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setImagenZonasUrl(dto.getImagenZonasUrl());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setEstadoEvento(dto.getEstadoEvento() != null ? dto.getEstadoEvento() : EstadoEvento.ACTIVO);
        evento.setAforoDisponible(dto.getAforoDisponible());
        evento.setMenoresDeEdadPermitidos(dto.getMenoresDeEdadPermitidos());
        evento.setRestricciones(dto.getRestricciones());
        evento.setPoliticasDevolucion(dto.getPoliticasDevolucion());
        evento.setLocal(local);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());

        /**Añadido para añadir tipos de tickets*/
        /*
        List<TipoTicket> tipos = new ArrayList<>();

        for (TipoTicketRequest tReq : dto.getTipoTickets()){
            TipoTicket tt = new TipoTicket();
            tt.setIdTipoTicket(tReq.getIdTipoTicket());
            tt.setNombre(tReq.getNombre());
            tt.setDescripcion(tReq.getDescripcion());
            tt.setPrecio(tReq.getPrecio());
            tt.setStock(tReq.getStock());
            tt.setLimitePorPersona(tReq.getLimitePorPersona());

            List<PrecioEscalonado> precios = new ArrayList<>();

            for (PrecioEscalonadoRequest pReq : tReq.getPreciosEscalonados()){
                PrecioEscalonado pe = new PrecioEscalonado();
                pe.setIdPrecio(pReq.getIdPrecio());
                pe.setNombreEtapa(pReq.getNombreEtapa());
                pe.setFechaInicio(pReq.getFechaInicio());
                pe.setFechaFin(pReq.getFechaFin());
                pe.setActivo(pReq.getActivo());
                precios.add(pe);
            }
            tt.setPreciosEscalonados(precios);
            tipos.add(tt);
        }
        evento.setTiposTicket(tipos);
        */

        return evento;
    }

    @AfterMapping
    protected void setearEventoEnTipoTickets(@MappingTarget Evento evento) {
        if (evento.getTiposTicket() != null) {
            for (TipoTicket tipo : evento.getTiposTicket()) {
                tipo.setEvento(evento);
            }
        }
    }

    @AfterMapping
    protected void setearTipoTicketEnPrecios(@MappingTarget TipoTicket tipoTicket) {
        if (tipoTicket.getPreciosEscalonados() != null) {
            for (PrecioEscalonado precio : tipoTicket.getPreciosEscalonados()) {
                precio.setTipoTicket(tipoTicket);
            }
        }
    }

    public void updateEntity(Evento evento, EventoCreateDTO dto, Local local) {
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFechaEvento(dto.getFechaEvento());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setImagenZonasUrl(dto.getImagenZonasUrl());
        evento.setMenoresDeEdadPermitidos(dto.getMenoresDeEdadPermitidos());
        evento.setRestricciones(dto.getRestricciones());
        evento.setPoliticasDevolucion(dto.getPoliticasDevolucion());
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

