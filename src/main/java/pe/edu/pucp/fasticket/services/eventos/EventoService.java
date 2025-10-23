package pe.edu.pucp.fasticket.services.eventos;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.events.EventoCanceladoEvent;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.EventoMapper;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;

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
    private final ApplicationEventPublisher eventPublisher;

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
        log.info("Creando nuevo evento en modo BORRADOR: {}", dto.getNombre());

        // Validar fecha futura
        if (dto.getFechaEvento().isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha del evento debe ser futura");
        }

        // Obtener y validar local
        Local local = null;
        if (dto.getIdLocal() != null) {
            local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + dto.getIdLocal()));

            // RF-006: Impedir asociar eventos a locales inactivos
            if (!local.getActivo()) {
                throw new BusinessException("No se puede asociar un evento a un local inactivo");
            }
        }

        // --- INICIO DE LA CORRECCIÓN ---

        // 1. Mapear la entidad (usando tu mapper)
        Evento evento = eventoMapper.toEntity(dto, local);

        // 2. ¡LA CORRECCIÓN CLAVE! Forzar el estado a BORRADOR.
        // Ignoramos lo que venga en el DTO (ej. "ACTIVO"),
        // el primer paso SIEMPRE es BORRADOR.
        evento.setEstadoEvento(EstadoEvento.BORRADOR);

        // 3. No está activo hasta que se publique
        evento.setActivo(false);

        // 4. Guardar el borrador en la BD
        Evento eventoGuardado = eventoRepository.save(evento);

        // --- FIN DE LA CORRECCIÓN ---

        log.info("Evento creado en modo BORRADOR con ID: {}", eventoGuardado.getIdEvento());

        // 5. Devolver el DTO con el ID (esto es vital para el frontend)
        return eventoMapper.toResponseDTO(eventoGuardado);
    }

    @Transactional
    public EventoResponseDTO actualizar(Integer id, EventoCreateDTO dto) {
        log.info("Actualizando evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        // Obtener y validar local
        Local local = null;
        if (dto.getIdLocal() != null) {
            local = localRepository.findById(dto.getIdLocal())
                    .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + dto.getIdLocal()));

            // RF-006: Impedir asociar eventos a locales inactivos
            if (!local.getActivo()) {
                throw new BusinessException("No se puede asociar un evento a un local inactivo");
            }
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

    /**
     * RF-065: Filtra eventos por tipo/categoría.
     *
     * @param tipoEvento Tipo de evento (CONCIERTO, TEATRO, DEPORTIVO, etc.)
     * @return Lista de eventos del tipo especificado
     */
    public List<EventoResponseDTO> listarPorTipo(String tipoEvento) {
        log.info("Buscando eventos por tipo: {}", tipoEvento);
        return eventoRepository.findByTipoEventoAndActivoTrue(tipoEvento).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-066: Filtra eventos por rango de fechas.
     *
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin    Fecha de fin del rango
     * @return Lista de eventos en el rango de fechas
     */
    public List<EventoResponseDTO> listarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Buscando eventos entre {} y {}", fechaInicio, fechaFin);
        return eventoRepository.findByFechaEventoBetweenAndActivoTrue(fechaInicio, fechaFin).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-067: Filtra eventos por ubicación (distrito del local).
     *
     * @param idDistrito ID del distrito
     * @return Lista de eventos en el distrito especificado
     */
    public List<EventoResponseDTO> listarPorDistrito(Integer idDistrito) {
        log.info("Buscando eventos en distrito ID: {}", idDistrito);
        return eventoRepository.findByLocalDistritoIdDistritoAndActivoTrue(idDistrito).stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-069: Ordena eventos por fecha de inicio.
     *
     * @return Lista de eventos ordenados por fecha ascendente
     */
    public List<EventoResponseDTO> listarOrdenadosPorFecha() {
        log.info("Listando eventos ordenados por fecha");
        return eventoRepository.findByActivoTrueOrderByFechaEventoAsc().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-016: Cancela un evento cambiando su estado a CANCELADO.
     * TODO: Implementar notificaciones a clientes que compraron tickets.
     *
     * @param id ID del evento a cancelar
     */
    @Transactional
    public void cancelarEvento(Integer id) {
        log.info("Cancelando evento ID: {}", id);

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        if (evento.getEstadoEvento() == EstadoEvento.CANCELADO) {
            throw new BusinessException("El evento ya está cancelado");
        }

        evento.setEstadoEvento(EstadoEvento.CANCELADO);
        evento.setFechaActualizacion(LocalDate.now());
        eventoRepository.save(evento);

        log.info("Evento cancelado: {}. Se deben enviar notificaciones a los compradores.", id);

        // RF-016: Publicar evento para notificar a los compradores (Patrón Observer)
        try {
            // Obtener emails de todos los clientes que compraron tickets para este evento
            List<String> emailsAfectados = evento.getTickets().stream()
                    .map(ticket -> ticket.getCliente().getEmail())
                    .distinct()
                    .collect(Collectors.toList());

            String motivo = "Cancelación administrativa del evento";

            log.info("📢 Publicando evento EventoCanceladoEvent. Afectados: {} clientes", emailsAfectados.size());
            eventPublisher.publishEvent(new EventoCanceladoEvent(evento, motivo, emailsAfectados));
        } catch (Exception e) {
            log.error("⚠️ Error al publicar evento de cancelación (no crítico): {}", e.getMessage());
        }
    }

    @Transactional
    public EventoResponseDTO publicarEvento(Integer idEvento) {
        log.info("Intentando publicar evento ID: {}", idEvento);

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        // 1. Validar estado
        if (evento.getEstadoEvento() != EstadoEvento.BORRADOR) {
            throw new BusinessException("Solo se pueden publicar eventos en estado BORRADOR");
        }

        // 2. Validar RF-014: Que tenga al menos una entrada/ticket
        // (Asumimos que la relación está en Evento.tickets)
        if (evento.getTickets() == null || evento.getTickets().isEmpty()) {
            // Este es el error POP UP que ve el frontend
            throw new BusinessException("Error: No se ha definido el mínimo de categorías de entradas.");
        }

        // 3. ¡Publicar!
        evento.setEstadoEvento(EstadoEvento.PUBLICADO);
        evento.setActivo(true);
        evento.setFechaActualizacion(LocalDate.now()); // Asegúrate de tener LocalDate.now()

        Evento eventoPublicado = eventoRepository.save(evento);

        log.info("¡Evento ID: {} publicado exitosamente!", idEvento);
        return eventoMapper.toResponseDTO(eventoPublicado);
    }
}

