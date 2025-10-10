package pe.edu.pucp.fasticket.controllers.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.services.eventos.TipoTicketServicio;

import java.util.List;

@RestController
@RequestMapping("api/v1/tipos_ticket")
@CrossOrigin(origins = "http://localhost:4200")
public class TipoTicketController {

    @Autowired
    private TipoTicketServicio serv_tipoTicket;

    @GetMapping("/listar_tipos_ticket")
    public ResponseEntity<List<TipoTicket>> listarTodos(){
        List<TipoTicket> tiposTicket = serv_tipoTicket.ListarTiposTicket();
        return new ResponseEntity<>(tiposTicket, HttpStatus.OK);
    }

    //PARA GUARDAR

    @PostMapping
    public ResponseEntity<TipoTicket> guardarTipoTicket(@RequestBody TipoTicket tipoTicket){
        try{
            TipoTicket nuevoTipoTicket = serv_tipoTicket.Guardar(tipoTicket);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTipoTicket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
