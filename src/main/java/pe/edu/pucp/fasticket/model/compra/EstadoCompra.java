package pe.edu.pucp.fasticket.model.compra;

/**
 * Estados posibles de una orden de compra.
 * RF-089: ANULADO para órdenes anuladas por el administrador.
 */
public enum EstadoCompra {
    /**
     * Pago aprobado y compra confirmada.
     */
    APROBADO,
    
    /**
     * Pago rechazado.
     */
    RECHAZADO,
    
    /**
     * Orden pendiente de pago.
     */
    PENDIENTE,
    
    /**
     * RF-089: Orden anulada por el administrador.
     */
    ANULADO
}
