package pe.edu.pucp.fasticket.controllers.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.services.eventos.EventoServicio;

import java.util.List;

@RestController
@RequestMapping("api/v1/eventos")
@CrossOrigin(origins = "http://localhost:4200")
public class EventoController {

    @Autowired
    private EventoServicio serv_evento;

    @GetMapping("/listar_evento")
    public ResponseEntity<List<Evento>> listarTodos(){
        List<Evento> eventos = serv_evento.ListarEventos();
        return new ResponseEntity<>(eventos, HttpStatus.OK);
    }

    //Para GUARDAR

    @PostMapping
    public ResponseEntity<Evento> guardarEvento(@RequestBody Evento evento) {
        try{
            Evento nuevoEvento = serv_evento.Guardar(evento);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEvento);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
