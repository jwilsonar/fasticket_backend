package pe.edu.pucp.fasticket.services.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.eventos.ActualizarTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.CrearTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TipoTicketServicio {

    @Autowired
    private TipoTicketRepository repo_tipoTicket;
    @Autowired
    private EventosRepositorio repo_evento;

    public List<TipoTicketDTO> ListarTiposTicket() {
        return repo_tipoTicket.findAll()
                .stream()
                .map(TipoTicketDTO::new)
                .toList();
    }

    public TipoTicketDTO BuscarId(Integer id) {
        TipoTicket tipoTicket = repo_tipoTicket.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id));
        return new TipoTicketDTO(tipoTicket);
    }

    @Transactional
    public TipoTicketDTO crearTipoTicket(CrearTipoTicketRequestDTO dto) {
        Evento evento = repo_evento.findById(dto.getIdEvento())
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + dto.getIdEvento()));
        TipoTicket nuevoTipoTicket = new TipoTicket();
        nuevoTipoTicket.setEvento(evento);
        nuevoTipoTicket.setNombre(dto.getNombre());
        nuevoTipoTicket.setDescripcion(dto.getDescripcion());
        nuevoTipoTicket.setPrecio(dto.getPrecio());
        nuevoTipoTicket.setStock(dto.getStock());
        nuevoTipoTicket.setCantidadDisponible(dto.getStock());
        nuevoTipoTicket.setCantidadVendida(0);
        nuevoTipoTicket.setActivo(true);
        nuevoTipoTicket.setFechaInicioVenta(dto.getFechaInicioVenta());
        nuevoTipoTicket.setFechaFinVenta(dto.getFechaFinVenta());
        TipoTicket guardado = repo_tipoTicket.save(nuevoTipoTicket);
        return new TipoTicketDTO(guardado);
    }

    @Transactional
    public TipoTicketDTO actualizarTipoTicket(Integer id, ActualizarTipoTicketRequestDTO dto) {
        TipoTicket tipoTicket = repo_tipoTicket.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id));
        tipoTicket.setNombre(dto.getNombre());
        tipoTicket.setDescripcion(dto.getDescripcion());
        tipoTicket.setPrecio(dto.getPrecio());
        tipoTicket.setFechaInicioVenta(dto.getFechaInicioVenta());
        tipoTicket.setFechaFinVenta(dto.getFechaFinVenta());
        tipoTicket.setStock(dto.getStock());
        TipoTicket actualizado = repo_tipoTicket.save(tipoTicket);
        return new TipoTicketDTO(actualizado);
    }

    public void Eliminar(Integer id) {
        if (!repo_tipoTicket.existsById(id)) {
            throw new ResourceNotFoundException("Tipo de ticket no encontrado con ID: " + id);
        }
        repo_tipoTicket.deleteById(id);
    }
}
