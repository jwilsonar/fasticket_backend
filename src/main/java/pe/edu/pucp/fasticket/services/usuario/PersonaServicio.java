package pe.edu.pucp.fasticket.services.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.pucp.fasticket.dto.LoginResponse;
import pe.edu.pucp.fasticket.dto.RegistroRequest;
import pe.edu.pucp.fasticket.dto.RegistroResponse;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PersonaServicio {
    @Autowired
    private PersonasRepositorio repo_personas;

    public List<Persona> ListarPersonas(){
        return repo_personas.findAll();
    }

    public Optional<Persona> BuscarId(Integer id){
        return repo_personas.findById(id);
    }

    public Persona Guardar(Persona persona){
        return (Persona) repo_personas.save(persona);
    }

    public void Eliminar(Integer id){
        repo_personas.deleteById(id);
    }

    // Método para login
    public LoginResponse login(String email, String contrasena) {
        try {
            // Buscar persona por email
            List<Persona> personas = repo_personas.findAll();
            Persona persona = null;
            
            for (Persona p : personas) {
                if (p.getEmail() != null && p.getEmail().equals(email)) {
                    persona = p;
                    break;
                }
            }
            
            if (persona == null) {
                return new LoginResponse(false, "Usuario no encontrado", null);
            }
            
            // Verificar contraseña
            if (persona.getContrasena() != null && persona.getContrasena().equals(contrasena)) {
                return new LoginResponse(true, "Login exitoso", persona);
            } else {
                return new LoginResponse(false, "Contraseña incorrecta", null);
            }
            
        } catch (Exception e) {
            return new LoginResponse(false, "Error interno del servidor", null);
        }
    }

    // Método para registrar cliente
    public RegistroResponse registrarCliente(RegistroRequest request) {
        try {
            // Verificar si el email ya existe
            List<Persona> personas = repo_personas.findAll();
            for (Persona p : personas) {
                if (p.getEmail() != null && p.getEmail().equals(request.getEmail())) {
                    return new RegistroResponse(request.getEmail(), "El email ya está registrado", false);
                }
            }
            
            // Crear nueva persona
            Persona nuevaPersona = new Persona();
            nuevaPersona.setDocIdentidad(request.getDocIdentidad());
            nuevaPersona.setNombres(request.getNombres());
            nuevaPersona.setApellidos(request.getApellidos());
            nuevaPersona.setTelefono(request.getTelefono());
            nuevaPersona.setEmail(request.getEmail());
            nuevaPersona.setDireccion(request.getDireccion());
            nuevaPersona.setContrasena(request.getContrasena());
            nuevaPersona.setFechaNacimiento(request.getFechaNacimiento());
            nuevaPersona.setTipoDocumento(request.getTipoDocumento());
            nuevaPersona.setRol(request.getRol());
            nuevaPersona.setActivo(true);
            nuevaPersona.setFechaCreacion(LocalDate.now());
            
            // Guardar la persona
            Persona personaGuardada = Guardar(nuevaPersona);
            
            return new RegistroResponse(personaGuardada.getEmail(), "Usuario registrado exitosamente", true);
            
        } catch (Exception e) {
            return new RegistroResponse(request.getEmail(), "Error al registrar usuario: " + e.getMessage(), false);
        }
    }
}
