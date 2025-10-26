package pe.edu.pucp.fasticket.events;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pe.edu.pucp.fasticket.model.eventos.Evento;

/**
 * Evento publicado cuando un evento es cancelado.
 * RF-016: Cancelar un evento, generando acciones de comunicación definidas.
 * 
 * Patrón Observer: Este evento es observado por NotificacionListener
 * para notificar a todos los compradores.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class EventoCanceladoEvent {
    
    private final Evento evento;
    private final String motivoCancelacion;
    private final List<String> emailsAfectados;
    
    // Getters explícitos para compatibilidad
    public Evento getEvento() {
        return evento;
    }
    
    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }
    
    public List<String> getEmailsAfectados() {
        return emailsAfectados;
    }
}

