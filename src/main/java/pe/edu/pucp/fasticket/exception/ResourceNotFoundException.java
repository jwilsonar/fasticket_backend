package pe.edu.pucp.fasticket.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no existe en la base de datos.
 * 
 * <p>Esta excepción debe ser capturada por el {@link GlobalExceptionHandler}
 * y convertida en una respuesta HTTP 404 (Not Found).</p>
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor con mensaje personalizado.
     * 
     * @param message Mensaje descriptivo del recurso no encontrado
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     * 
     * @param message Mensaje descriptivo
     * @param cause Causa raíz de la excepción
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

