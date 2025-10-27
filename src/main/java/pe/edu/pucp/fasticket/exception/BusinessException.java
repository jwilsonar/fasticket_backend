package pe.edu.pucp.fasticket.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 * 
 * <p>Esta excepción debe ser capturada por el {@link GlobalExceptionHandler}
 * y convertida en una respuesta HTTP 409 (Conflict) o 400 (Bad Request).</p>
 * 
 * <p>Ejemplos de uso:</p>
 * <ul>
 *   <li>Intentar crear un local con nombre duplicado</li>
 *   <li>Intentar comprar tickets cuando el evento está agotado</li>
 *   <li>Intentar aplicar un descuento expirado</li>
 * </ul>
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor con mensaje personalizado.
     * 
     * @param message Mensaje descriptivo de la violación de regla de negocio
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     * 
     * @param message Mensaje descriptivo
     * @param cause Causa raíz de la excepción
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

