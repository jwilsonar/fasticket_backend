package pe.edu.pucp.fasticket.dto.eventos;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Respuesta con datos de un local")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalResponseDTO {
    
    @Schema(description = "ID del local", example = "1")
    private Integer idLocal;
    
    @Schema(description = "Nombre del local", example = "Estadio Nacional")
    private String nombre;
    
    @Schema(description = "Dirección", example = "Av. José Díaz, Lima")
    private String direccion;
    
    @Schema(description = "URL del mapa", example = "https://maps.google.com/...")
    private String urlMapa;
    
    @Schema(description = "Aforo total", example = "45000")
    private Integer aforoTotal;
    
    @Schema(description = "Estado activo", example = "true")
    private Boolean activo;
    
    @Schema(description = "ID del distrito", example = "1")
    private Integer idDistrito;
    
    @Schema(description = "Nombre del distrito", example = "Lima")
    private String nombreDistrito;
    
    @Schema(description = "Fecha de creación", example = "2025-10-10")
    private LocalDate fechaCreacion;
    
    @Schema(description = "URL de la imagen del local", example = "https://bucket.s3.region.amazonaws.com/locales/1/imagen.jpg")
    private String imagenUrl;
}

