package pe.edu.pucp.fasticket.dto.usuario;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;

/**
 * DTO para mostrar el perfil del administrador.
 * Información del administrador del sistema.
 */
@Schema(description = "Información del perfil del administrador")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdministradorPerfilResponseDTO {
    
    @Schema(description = "ID del administrador", example = "1")
    private Integer idAdministrador;
    
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
    
    @Schema(description = "Email", example = "admin@pucp.edu.pe")
    private String email;
    
    @Schema(description = "Fecha de nacimiento", example = "1990-05-15")
    private LocalDate fechaNacimiento;
    
    @Schema(description = "Dirección", example = "Av. Principal 123, Lima")
    private String direccion;
    
    @Schema(description = "Cargo del administrador", example = "Administrador del Sistema")
    private String cargo;
    
    @Schema(description = "Edad actual calculada", example = "34")
    private Integer edad;
    
    @Schema(description = "Estado activo", example = "true")
    private Boolean activo;
}
