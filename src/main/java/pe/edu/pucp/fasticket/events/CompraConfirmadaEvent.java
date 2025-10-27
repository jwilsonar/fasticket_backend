package pe.edu.pucp.fasticket.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

/**
 * Evento publicado cuando una compra es confirmada.
 * RF-049, RF-086: Notificar por correo la confirmación de compra.
 * 
 * Patrón Observer: Este evento es observado por NotificacionListener.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class CompraConfirmadaEvent {
    
    private final OrdenCompra orden;
    private final String emailCliente;
    
    // Getters explícitos para compatibilidad
    public OrdenCompra getOrden() {
        return orden;
    }
    
    public String getEmailCliente() {
        return emailCliente;
    }
}

