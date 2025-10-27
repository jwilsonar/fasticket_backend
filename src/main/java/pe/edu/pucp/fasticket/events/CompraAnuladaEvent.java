package pe.edu.pucp.fasticket.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;

/**
 * Evento publicado cuando una compra es anulada por el administrador.
 * RF-089: Notificar al cliente sobre la anulación.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class CompraAnuladaEvent {
    
    private final OrdenCompra orden;
    private final String emailCliente;
    private final String motivoAnulacion;
    
    // Getters explícitos para compatibilidad
    public OrdenCompra getOrden() {
        return orden;
    }
    
    public String getEmailCliente() {
        return emailCliente;
    }
    
    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }
}

