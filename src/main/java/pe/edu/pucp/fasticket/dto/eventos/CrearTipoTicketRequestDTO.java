package pe.edu.pucp.fasticket.dto.eventos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import org.joda.time.DateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para crear TipoTicket")
public class CrearTipoTicketRequestDTO {
    
    @Schema(description = "ID de la zona", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID de la zona es obligatorio")
    private Integer idZona;

    @Schema(description = "Nombre del tipo de ticket", example = "VIP", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Schema(description = "Descripción del tipo de ticket", example = "Acceso VIP con beneficios exclusivos")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    @Schema(description = "Precio del ticket", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a cero")
    private Double precio;

    @Schema(description = "Stock disponible", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El stock es obligatorio")
    @Positive(message = "El stock debe ser mayor a cero")
    private Integer stock;
    
    @Schema(description = "Límite de tickets por persona", example = "4")
    @Positive(message = "El límite por persona debe ser mayor a cero")
    private Integer limitePorPersona;

    @Schema(description = "Fecha de inicio de venta del ticket", example = "2024-07-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La fecha de inicio de venta es obligatoria")
    private LocalDate fechaInicioVenta;

    @Schema(description = "Fecha de fin de venta del ticket", example = "2024-07-31", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La fecha de fin de venta es obligatoria")
    private LocalDate fechaFinVenta;

    @Valid
    private List<RangoPrecio> preciosEscalonados;

    @Data
    public static class RangoPrecio {
        @NotBlank private String nombreEtapa;
        @NotNull @Positive private Double precio;
        @NotNull private LocalDateTime fechaInicio;
        @NotNull private LocalDateTime fechaFin;
    }
}
