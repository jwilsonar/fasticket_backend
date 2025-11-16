package pe.edu.pucp.fasticket.dto.eventos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Respuesta con datos de un evento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponseDTO {
    
    @Schema(description = "ID del evento", example = "1")
    private Integer idEvento;
    
    @Schema(description = "Nombre del evento", example = "Concierto Rock 2025")
    private String nombre;
    
    @Schema(description = "Descripción", example = "Gran concierto de rock")
    private String descripcion;
    
    @Schema(description = "Fecha del evento", example = "2025-12-31")
    private LocalDate fechaEvento;
    
    @Schema(description = "Hora de inicio", example = "20:00")
    private LocalTime horaInicio;
    
    @Schema(description = "Hora de fin", example = "23:00")
    private LocalTime horaFin;
    
    @Schema(description = "URL de imagen", example = "https://example.com/imagen.jpg")
    private String imagenUrl;

    @Schema(description = "URL de imagen de zonas", example = "https://example.com/imagen_zonas.jpg")
    private String imagenZonasUrl;
    
    @Schema(description = "Tipo de evento", example = "CONCIERTO")
    private TipoEvento tipoEvento;
    
    @Schema(description = "Estado del evento", example = "ACTIVO")
    private EstadoEvento estadoEvento;
    
    @Schema(description = "Aforo disponible", example = "5000")
    private Integer aforoDisponible;
    
    @Schema(description = "Estado activo", example = "true")
    private Boolean activo;
    
    @Schema(description = "ID del local", example = "1")
    private Integer idLocal;
    
    @Schema(description = "Nombre del local", example = "Estadio Nacional")
    private String nombreLocal;
    
    @Schema(description = "Fecha de creación", example = "2025-10-10")
    private LocalDate fechaCreacion;

    @Schema(description = "Indica si se permiten menores de edad en el evento", example = "false")
    private Boolean menoresDeEdadPermitidos;

    @Schema(description = "Restricciones del evento", example = "Prohibido el ingreso de menores de 18 años")
    @Size(max = 500, message = "Las restricciones no pueden exceder 500 caracteres")
    private String restricciones;

    @Schema(description = "Políticas de devolución del evento", example = "No se permiten devoluciones")
    @Size(max = 1000, message = "Las políticas de devolución no pueden exceder 1000 caracteres")
    private String politicasDevolucion;

    @Schema(description = "Lista de los tipos de tickets", example = "nombre: VIP, stock: 200, etc")
    private List<TipoTicket> tipoTickets;
}

