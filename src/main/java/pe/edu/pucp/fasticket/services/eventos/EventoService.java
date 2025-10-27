package pe.edu.pucp.fasticket.services.eventos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.EventoMapper;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio completo para la gestión de eventos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventoService {

    private final EventosRepositorio eventoRepository;
    private final LocalesRepositorio localRepository;
    private final EventoMapper eventoMapper;

    public List<EventoResponseDTO> listarTodos() {
        return eventoRepository.findAll().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventoResponseDTO> listarActivos() {
        return eventoRepository.findByActivoTrue().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventoResponseDTO> listarProximos() {
        return eventoRepository.findEventosProximos(LocalDate.now()).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventoResponseDTO> listarPorEstado(EstadoEvento estado) {
        return eventoRepository.findByEstadoEventoAndActivoTrue(estado).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EventoResponseDTO obtenerPorId(Integer id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
        return eventoMapper.toResponseDTO(evento);
    }

    @Transactional
    public EventoResponseDTO crear(EventoCreateDTO dto) {
        log.info("Creando nuevo evento: {}", dto.getNombre());

        // Validar fecha futura
        if (dto.getFechaEvento().isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha del evento debe ser futura");
        }

        // Obtener local
        Local local = null;
        if (dto.getIdLocal() != null) {
            local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + dto.getIdLocal()));
        }

        // Crear y guardar
        Evento evento = eventoMapper.toEntity(dto, local);
        Evento eventoGuardado = eventoRepository.save(evento);

        log.info("Evento creado con ID: {}", eventoGuardado.getIdEvento());
        return eventoMapper.toResponseDTO(eventoGuardado);
    }

    @Transactional
    public EventoResponseDTO actualizar(Integer id, EventoCreateDTO dto) {
        log.info("Actualizando evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        // Obtener local
        Local local = null;
        if (dto.getIdLocal() != null) {
            local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + dto.getIdLocal()));
        }

        // Actualizar
        eventoMapper.updateEntity(evento, dto, local);
        Evento eventoActualizado = eventoRepository.save(evento);

        log.info("Evento actualizado: {}", id);
        return eventoMapper.toResponseDTO(eventoActualizado);
    }

    @Transactional
    public void eliminarLogico(Integer id) {
        log.info("Eliminación lógica del evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        evento.setActivo(false);
        evento.setFechaActualizacion(LocalDate.now());
        eventoRepository.save(evento);

        log.info("Evento desactivado: {}", id);
    }
}

