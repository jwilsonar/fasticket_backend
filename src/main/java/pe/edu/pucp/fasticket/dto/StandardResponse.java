package pe.edu.pucp.fasticket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase genérica para respuestas estándar de la API
 * 
 * @param <T> Tipo de dato contenido en la respuesta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de la API")
public class StandardResponse<T> {
    
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private Boolean ok;
    
    @Schema(description = "Mensaje descriptivo de la respuesta", example = "Operación exitosa")
    private String mensaje;
    
    @Schema(description = "Datos de la respuesta")
    private T data;
    
    /**
     * Crea una respuesta exitosa con datos
     */
    public static <T> StandardResponse<T> success(String mensaje, T data) {
        return StandardResponse.<T>builder()
                .ok(true)
                .mensaje(mensaje)
                .data(data)
                .build();
    }
    
    /**
     * Crea una respuesta exitosa sin datos
     */
    public static <T> StandardResponse<T> success(String mensaje) {
        return StandardResponse.<T>builder()
                .ok(true)
                .mensaje(mensaje)
                .build();
    }
    
    /**
     * Crea una respuesta de error
     */
    public static <T> StandardResponse<T> error(String mensaje) {
        return StandardResponse.<T>builder()
                .ok(false)
                .mensaje(mensaje)
                .build();
    }
    
    /**
     * Crea una respuesta de error con datos adicionales
     */
    public static <T> StandardResponse<T> error(String mensaje, T data) {
        return StandardResponse.<T>builder()
                .ok(false)
                .mensaje(mensaje)
                .data(data)
                .build();
    }
}

