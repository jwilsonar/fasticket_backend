package pe.edu.pucp.fasticket.dto.auditoria;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ErrorLogDetalleDTO {
    private Integer idError;
    private LocalDateTime fechaHora;
    private String severidad;
    private String modulo;
    private String mensajeBreve;
    private String detalleTecnico;
    private String nombreAdmin;
    private String traza; // <-- El campo que faltaba para el modal
}