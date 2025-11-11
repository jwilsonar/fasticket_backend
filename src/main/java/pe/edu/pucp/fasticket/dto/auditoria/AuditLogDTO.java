package pe.edu.pucp.fasticket.dto.auditoria;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogDTO {

    private Integer idAudit;
    private LocalDateTime fechaHora;
    private String accion;
    private String modulo;
    private String detalle;

    // Para saber quién lo hizo, solo traemos el email
    private String adminEmail;
}