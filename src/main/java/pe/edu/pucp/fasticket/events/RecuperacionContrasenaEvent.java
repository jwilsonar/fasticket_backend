package pe.edu.pucp.fasticket.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Evento publicado cuando se solicita recuperación de contraseña.
 * RF-052: Enviar confirmación de recuperación de contraseña.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class RecuperacionContrasenaEvent {
    
    private final String email;
    private final String nombreCompleto;
    private final String tokenRecuperacion;
    
    // Getters explícitos para compatibilidad
    public String getEmail() {
        return email;
    }
    
    public String getNombreCompleto() {
        return nombreCompleto;
    }
    
    public String getTokenRecuperacion() {
        return tokenRecuperacion;
    }
}

