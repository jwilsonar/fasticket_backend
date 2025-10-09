package pe.edu.pucp.fasticket.controllers.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.LoginRequest;
import pe.edu.pucp.fasticket.dto.LoginResponse;
import pe.edu.pucp.fasticket.dto.RegistroRequest;
import pe.edu.pucp.fasticket.dto.RegistroResponse;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.services.usuario.PersonaServicio;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/personas")
@CrossOrigin(origins = "http://localhost:4200")
public class PersonaController {

    @Autowired
    private PersonaServicio serv_persona;

    @GetMapping("/listar_persona")
    public ResponseEntity<List<Persona>> listarTodos(){
        List<Persona> personas = serv_persona.ListarPersonas();
        return new ResponseEntity<>(personas, HttpStatus.OK);
    }


    // ========== LOGIN ==========
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse resp = serv_persona.login(request.getEmail(), request.getContrasena());
        if (resp.isOk()) {
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
    }

    // ========== REGISTRO ==========
    @PostMapping("/registrar")
    public ResponseEntity<RegistroResponse> registrar(@RequestBody RegistroRequest request) {
        try {
            RegistroResponse resp = serv_persona.registrarCliente(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            RegistroResponse error = new RegistroResponse();
            error.setEmail(request.getEmail());
            error.setMensaje("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
