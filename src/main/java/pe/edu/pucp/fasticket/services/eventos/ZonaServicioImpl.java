package pe.edu.pucp.fasticket.services.eventos;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZonaServicioImpl implements ZonaServicio {

    private final ZonaRepository zonaRepository;
    private final EventosRepositorio eventoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Zona> listarTodas() {
        log.info("Listando todas las zonas");
        return zonaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Zona> buscarPorId(Integer id) {
        log.info("Buscando zona con ID: {}", id);
        return zonaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zona> buscarPorEvento(Integer idEvento) {
        log.info("Buscando zonas del evento con ID: {}", idEvento);
        return zonaRepository.findByEvento_IdEvento(idEvento); // <-- Buscar por Evento
    }

    @Override
    public Zona crear(Zona zona, Integer idEvento) {
        log.info("Creando nueva zona: {}", zona.getNombre());
        log.info("ID del evento a asignar: {}", idEvento);

        if (idEvento == null) {
            throw new BusinessException("El ID del evento no puede ser nulo");
        }

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        Local local = evento.getLocal();
        if (local == null) {
            throw new BusinessException("El evento ID: " + idEvento + " no tiene un local asociado.");
        }
        if (local.getAforoTotal() == null || local.getAforoTotal() <= 0) {
            throw new BusinessException("El local '" + local.getNombre() + "' no tiene un aforo total definido.");
        }

        Integer aforoZonasActual = evento.getZonas().stream()
                .mapToInt(Zona::getAforoMax)
                .sum();

        log.info("Aforo actual del evento {}: {}. Aforo del local: {}", idEvento, aforoZonasActual, local.getAforoTotal());

        if ((aforoZonasActual + zona.getAforoMax()) > local.getAforoTotal()) {
            throw new BusinessException(
                    String.format("Se ha superado el aforo máximo del local (%d). Aforo actual de zonas: %d. Aforo nuevo: %d",
                            local.getAforoTotal(),
                            aforoZonasActual,
                            zona.getAforoMax()
                    )
            );
        }
        zona.setEvento(evento);

        Zona zonaGuardada = zonaRepository.save(zona);
        log.info("Zona guardada - ID: {}, Evento: {}", zonaGuardada.getIdZona(), zonaGuardada.getEvento().getIdEvento());
        return zonaGuardada;
    }

    @Override
    public Zona actualizar(Zona zona, Integer idEvento) {
        log.info("Actualizando zona con ID: {}", zona.getIdZona());
        log.info("ID del evento a asignar: {}", idEvento);

        if (idEvento != null) {
            log.info("Buscando evento con ID: {}", idEvento);
            Evento evento = eventoRepository.findById(idEvento)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));
            zona.setEvento(evento);
            log.info("Evento asignado: {}", evento.getNombre());
        } else {
            log.warn("El ID del evento es null");
        }

        return zonaRepository.save(zona);
    }

    @Override
    public void eliminar(Integer id) {
        log.info("Eliminando zona con ID: {}", id);
        if (!zonaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zona no encontrada con ID: " + id);
        }
        zonaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zona> buscarActivas() {
        log.info("Buscando zonas activas");
        return zonaRepository.findByActivoTrue();
    }

}
