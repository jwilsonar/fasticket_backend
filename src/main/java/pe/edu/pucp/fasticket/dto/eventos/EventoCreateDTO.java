package pe.edu.pucp.fasticket.dto.eventos;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;

@Schema(description = "Datos para crear un nuevo evento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoCreateDTO {
    
    @Schema(description = "Nombre del evento", example = "Concierto Rock 2025", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    private String nombre;

    @Schema(description = "Descripción del evento", example = "Gran concierto de rock con las mejores bandas")
    @Size(max = 2000, message = "La descripción no puede exceder 2000 caracteres")
    private String descripcion;

    @Schema(description = "Fecha del evento", example = "2025-12-31", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La fecha es obligatoria")
    @Future(message = "La fecha debe ser futura")
    private LocalDate fechaEvento;

    @Schema(description = "Hora de inicio", example = "20:00")
    private LocalTime horaInicio;

    @Schema(description = "Hora de fin", example = "23:00")
    private LocalTime horaFin;

    @Schema(description = "URL de la imagen", example = "https://example.com/imagen.jpg")
    @Size(max = 500, message = "La URL no puede exceder 500 caracteres")
    private String imagenUrl;

    @Schema(description = "Tipo de evento", example = "CONCIERTO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEvento tipoEvento;

    @Schema(description = "Estado del evento", example = "ACTIVO")
    private EstadoEvento estadoEvento;

    @Schema(description = "Aforo disponible", example = "5000")
    @Positive(message = "El aforo debe ser positivo")
    private Integer aforoDisponible;

    @Schema(description = "ID del local", example = "1")
    private Integer idLocal;

    @Schema(description = "RF-072: Edad mínima para asistir al evento", example = "18")
    @Min(value = 0, message = "La edad mínima no puede ser negativa")
    @Max(value = 100, message = "La edad mínima no puede exceder 100 años")
    private Integer edadMinima = 0;

    @Schema(description = "RF-072: Restricciones del evento", example = "No se permite el ingreso de alimentos ni bebidas")
    @Size(max = 1000, message = "Las restricciones no pueden exceder 1000 caracteres")
    private String restricciones;

    @Schema(description = "RF-073: Políticas de devolución/cambio", example = "No se permiten devoluciones después de 24 horas")
    @Size(max = 1000, message = "Las políticas no pueden exceder 1000 caracteres")
    private String politicasDevolucion;
}

