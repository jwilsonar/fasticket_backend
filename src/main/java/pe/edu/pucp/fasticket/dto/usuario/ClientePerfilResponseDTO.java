package pe.edu.pucp.fasticket.dto.usuario;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.model.usuario.TipoNivel;

/**
 * DTO para mostrar el perfil del cliente.
 * RF-030, RF-033: Información del cliente y puntos acumulados.
 */
@Schema(description = "Información del perfil del cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientePerfilResponseDTO {
    
    @Schema(description = "ID del cliente", example = "1")
    private Integer idCliente;
    
    @Schema(description = "Tipo de documento", example = "DNI")
    private TipoDocumento tipoDocumento;
    
    @Schema(description = "Número de documento", example = "12345678")
    private String docIdentidad;
    
    @Schema(description = "Nombres", example = "Juan Carlos")
    private String nombres;
    
    @Schema(description = "Apellidos", example = "García López")
    private String apellidos;
    
    @Schema(description = "Teléfono", example = "+51987654321")
    private String telefono;
    
    @Schema(description = "Email", example = "juan.garcia@example.com")
    private String email;
    
    @Schema(description = "Fecha de nacimiento", example = "1990-05-15")
    private LocalDate fechaNacimiento;
    
    @Schema(description = "Dirección", example = "Av. Principal 123, Lima")
    private String direccion;
    
    @Schema(description = "RF-033: Puntos de fidelización acumulados", example = "1500")
    private Integer puntosAcumulados;
    
    @Schema(description = "Nivel del cliente (CLASICO, PLATA, ORO)", example = "ORO")
    private TipoNivel nivel;
    
    @Schema(description = "Edad actual calculada", example = "34")
    private Integer edad;
}

