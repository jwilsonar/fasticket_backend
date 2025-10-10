package pe.edu.pucp.fasticket.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Manejador global de excepciones para toda la aplicación.
 * 
 * <p>Captura todas las excepciones lanzadas en los controllers y las convierte
 * en respuestas HTTP estandarizadas con el formato {@link ErrorResponse}.</p>
 * 
 * <p>Beneficios:</p>
 * <ul>
 *   <li>Respuestas de error consistentes en toda la API</li>
 *   <li>Logging centralizado de errores</li>
 *   <li>Mejor experiencia para desarrolladores que consumen la API</li>
 *   <li>Ocultación de detalles técnicos sensibles</li>
 * </ul>
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones cuando un recurso no es encontrado.
     * 
     * @param ex Excepción de recurso no encontrado
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 404 y detalles del error
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        
        log.error("Recurso no encontrado: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Maneja excepciones de violación de reglas de negocio.
     * 
     * @param ex Excepción de negocio
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 409 y detalles del error
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, 
            HttpServletRequest request) {
        
        log.error("Error de regla de negocio: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Business Rule Violation")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja errores de validación de Bean Validation (@Valid).
     * 
     * <p>Captura errores como @NotNull, @Size, @Email, etc.</p>
     * 
     * @param ex Excepción de validación
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 400 y mapa de errores por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.error("Errores de validación: {}", fieldErrors);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("message", "Error en la validación de los datos de entrada");
        response.put("path", request.getRequestURI());
        response.put("errors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja errores de argumento ilegal (parámetros inválidos).
     * 
     * @param ex Excepción de argumento ilegal
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 400 y detalles del error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        log.error("Argumento ilegal: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja cualquier excepción no capturada específicamente.
     * 
     * <p>Actúa como red de seguridad para evitar exponer detalles técnicos.</p>
     * 
     * @param ex Excepción genérica
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 500 y mensaje genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Error inesperado: ", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Ha ocurrido un error inesperado. Por favor, contacte al administrador.")
                .path(request.getRequestURI())
                .details(ex.getClass().getSimpleName())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

