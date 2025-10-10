package pe.edu.pucp.fasticket.controllers.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.eventos.EventoDetalleDTO;
import pe.edu.pucp.fasticket.services.eventos.EventoServicio;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "http://localhost:4200")
public class EventoController {

    private final EventoServicio eventoServicio;

    public EventoController(EventoServicio eventoServicio) {
        this.eventoServicio = eventoServicio;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDetalleDTO> obtenerDetalle(@PathVariable Integer id) {
        return ResponseEntity.ok(eventoServicio.obtenerDetallePorId(id));
    }
}
