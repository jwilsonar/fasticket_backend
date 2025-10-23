package pe.edu.pucp.fasticket.services.tickets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.tickets.TicketCreateDTO;
import pe.edu.pucp.fasticket.dto.tickets.TicketDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
// import pe.edu.pucp.fasticket.model.eventos.TipoEstadoTiket; // <--- LÍNEA ELIMINADA (EL ERROR)
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonasRepositorio;
import pe.edu.pucp.fasticket.mapper.TicketMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepositorio ticketRepository;
    private final EventosRepositorio eventoRepository;
    private final ZonasRepositorio zonaRepository;
    private final TicketMapper ticketMapper;

    @Transactional
    public TicketDTO agregarEntradaAEvento(Integer idEvento, TicketCreateDTO ticketDTO) {
        log.info("Agregando entrada '{}' al evento ID: {}", ticketDTO.getNombre(), idEvento);

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        if (evento.getEstadoEvento() != EstadoEvento.BORRADOR) {
            throw new BusinessException("Solo se pueden agregar entradas a eventos en estado BORRADOR");
        }

        Zona zona = zonaRepository.findById(ticketDTO.getIdZona())
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada con ID: " + ticketDTO.getIdZona()));

        if (ticketDTO.getStock() > zona.getAforoMax()) {
            throw new BusinessException("El stock (" + ticketDTO.getStock() + ") no puede superar el aforo de la zona '" + zona.getNombre() + "' (" + zona.getAforoMax() + ")");
        }

        Ticket nuevoTicket = ticketMapper.toEntity(ticketDTO, evento, zona);

        // --- CORRECCIÓN AQUÍ ---
        // Cambiamos el ENUM por el String exacto de tu base de datos
        nuevoTicket.setEstado("DISPONIBLE");
        // --- FIN DE LA CORRECCIÓN ---

        nuevoTicket.setActivo(true);

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        log.info("Ticket creado con ID: {}", ticketGuardado.getIdTicket());
        return ticketMapper.toDTO(ticketGuardado);
    }
}