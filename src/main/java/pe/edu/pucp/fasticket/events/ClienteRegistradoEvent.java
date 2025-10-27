package pe.edu.pucp.fasticket.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Evento publicado cuando un nuevo cliente se registra.
 * RF-048: Enviar correo de verificación de cuenta al registrarse un cliente.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class ClienteRegistradoEvent {
    
    private final String email;
    private final String nombreCompleto;
    private final String tokenVerificacion;
    
    // Getters explícitos para compatibilidad
    public String getEmail() {
        return email;
    }
    
    public String getNombreCompleto() {
        return nombreCompleto;
    }
    
    public String getTokenVerificacion() {
        return tokenVerificacion;
    }
}

