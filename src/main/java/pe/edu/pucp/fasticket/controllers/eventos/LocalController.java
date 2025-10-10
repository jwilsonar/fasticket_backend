package pe.edu.pucp.fasticket.controllers.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.services.eventos.LocalServicio;

import java.util.List;

@RestController
@RequestMapping("api/v1/locales") //Cambiar esto a eventos, o tal vez no, no estoy seguro
@CrossOrigin(origins = "http://localhost:4200")
public class LocalController {

    @Autowired
    private LocalServicio serv_local;

    @GetMapping("/listar_local")
    public ResponseEntity<List<Local>> listarTodos(){
        List<Local> locales = serv_local.ListarLocales();
        return new ResponseEntity<>(locales, HttpStatus.OK);
    }

    //Para GUARDAR

    @PostMapping
    public ResponseEntity<Local> guardarLocal(@RequestBody Local local){
        try{
            Local nuevoLocal = serv_local.Guardar(local);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLocal);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}