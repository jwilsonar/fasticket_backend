package pe.edu.pucp.fasticket.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.StandardResponse;

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
    public ResponseEntity<StandardResponse<ErrorResponse>> handleResourceNotFoundException(
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
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(StandardResponse.error(ex.getMessage(), error));
    }

    /**
     * Maneja excepciones de violación de reglas de negocio.
     * 
     * @param ex Excepción de negocio
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 409 y detalles del error
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleBusinessException(
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
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(StandardResponse.error(ex.getMessage(), error));
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
    public ResponseEntity<StandardResponse<Map<String, Object>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.error("Errores de validación: {}", fieldErrors);
        
        // Obtener el primer error para mostrarlo en el mensaje principal
        String firstErrorMessage = fieldErrors.values().iterator().next();
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("status", HttpStatus.BAD_REQUEST.value());
        errorDetails.put("error", "Validation Error");
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("errors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error(firstErrorMessage, errorDetails));
    }

    /**
     * Maneja errores de argumento ilegal (parámetros inválidos).
     * 
     * @param ex Excepción de argumento ilegal
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 400 y detalles del error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleIllegalArgumentException(
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
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error(ex.getMessage(), error));
    }

    /**
     * Maneja errores de credenciales inválidas (Spring Security).
     * 
     * @param ex Excepción de credenciales incorrectas
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 401 y detalles del error
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {
        
        log.error("Credenciales inválidas: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Credenciales inválidas")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(StandardResponse.error("Credenciales inválidas", error));
    }

    /**
     * Maneja errores de acceso denegado (Spring Security).
     * 
     * @param ex Excepción de autorización denegada
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 403 y detalles del error
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {
        
        log.error("Acceso denegado: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("No tiene permisos para acceder a este recurso")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(StandardResponse.error("No tiene permisos para acceder a este recurso", error));
    }

    /**
     * Maneja errores de recurso no encontrado (endpoint inexistente o Swagger deshabilitado).
     * 
     * @param ex Excepción de recurso no encontrado
     * @param request Request HTTP que generó el error
     * @return ResponseEntity con código 404 y detalles del error
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        
        String path = request.getRequestURI();
        String message = "Recurso no encontrado";
        
        // Mensaje específico si intenta acceder a Swagger deshabilitado
        if (path.contains("/swagger-ui") || path.contains("/api-docs")) {
            message = "La documentación Swagger está deshabilitada en este ambiente. " +
                      "Para habilitarla, configure SWAGGER_ENABLED=true en las variables de entorno.";
            log.warn("Intento de acceso a Swagger deshabilitado: {}", path);
        } else {
            log.error("Recurso no encontrado: {}", path);
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(message)
                .path(path)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(StandardResponse.error(message, error));
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
    public ResponseEntity<StandardResponse<ErrorResponse>> handleGenericException(
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
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Ha ocurrido un error inesperado. Por favor, contacte al administrador.", error));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardResponse<ErrorResponse>> handleGenericRuntimeError(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Error de Runtime detectado: {}", ex.getMessage()); // Loguea como error
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value()) // Estado 400
                .error("Bad Request")
                .message(ex.getMessage()) // Mensaje específico del error
                .path(request.getRequestURI())
                .build();
        return ResponseEntity
                .badRequest() // Devuelve 400
                .body(StandardResponse.error(ex.getMessage(), error));
    }
}

