package pe.edu.pucp.fasticket.dto.auditoria;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ErrorLogDTO {
    // DTO simple para no exponer el modelo completo
    private Integer idError;
    private LocalDateTime fechaHora;
    private String severidad;
    private String modulo;
    private String mensajeBreve;
    private String detalleTecnico;
    private String nombreAdmin; // Nombre del admin (si lo hay)
}