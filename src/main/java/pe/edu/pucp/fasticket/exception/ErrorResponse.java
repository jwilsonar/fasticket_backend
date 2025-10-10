package pe.edu.pucp.fasticket.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Clase para estandarizar las respuestas de error en toda la API.
 * 
 * <p>Proporciona información detallada sobre errores HTTP para facilitar
 * el debugging y mejorar la experiencia del desarrollador.</p>
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Schema(description = "Estructura estándar de respuesta de error")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @Schema(
        description = "Timestamp cuando ocurrió el error",
        example = "2025-10-10T15:30:45",
        type = "string",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @Schema(
        description = "Código de estado HTTP",
        example = "404"
    )
    private Integer status;
    
    @Schema(
        description = "Tipo de error HTTP",
        example = "Not Found"
    )
    private String error;
    
    @Schema(
        description = "Mensaje descriptivo del error",
        example = "Local no encontrado con ID: 1"
    )
    private String message;
    
    @Schema(
        description = "Ruta del endpoint que generó el error",
        example = "/api/v1/locales/1"
    )
    private String path;
    
    @Schema(
        description = "Detalles adicionales del error (opcional)",
        example = "El recurso solicitado no existe en la base de datos"
    )
    private String details;
}

