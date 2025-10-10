package pe.edu.pucp.fasticket.services.eventos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de eventos.
 * Maneja la lógica de negocio relacionada con eventos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventoServicio {

    private final EventosRepositorio eventosRepositorio;

    public List<Evento> listarTodos() {
        return eventosRepositorio.findAll();
    }

    public Optional<Evento> buscarPorId(Integer id) {
        return eventosRepositorio.findById(id);
    }

    public Evento obtenerPorId(Integer id) {
        return eventosRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
    }

    @Transactional
    public Evento guardar(Evento evento) {
        return eventosRepositorio.save(evento);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!eventosRepositorio.existsById(id)) {
            throw new ResourceNotFoundException("Evento no encontrado con ID: " + id);
        }
        eventosRepositorio.deleteById(id);
    }

    public List<Evento> buscarActivos() {
        return eventosRepositorio.findByActivoTrue();
    }
}
