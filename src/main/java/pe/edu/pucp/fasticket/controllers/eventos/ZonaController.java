package pe.edu.pucp.fasticket.controllers.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.services.eventos.ZonaServicio;

import java.util.List;

@RestController
@RequestMapping("api/zonas")
@CrossOrigin(origins = "http://localhost:4200")
public class ZonaController {

    @Autowired
    private ZonaServicio serv_zona;

    @GetMapping("/listar_zona")
    public ResponseEntity<List<Zona>> listarTodos(){
        List<Zona> zonas = serv_zona.ListarZonas();
        return new ResponseEntity<>(zonas, HttpStatus.OK);
    }

    //PARA GUARDAR

    @PostMapping
    public ResponseEntity<Zona> guardarZona(@RequestBody Zona zona){
        try{
            Zona nuevaZona = serv_zona.Guardar(zona);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaZona);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
