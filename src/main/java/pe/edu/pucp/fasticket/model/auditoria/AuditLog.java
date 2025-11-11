package pe.edu.pucp.fasticket.model.auditoria;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.Administrador; // Asumimos que quieres ligarlo al admin
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Integer idAudit;

    @Column(name = "fecha_hora", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaHora;

    @Column(name = "accion")
    private String accion; // Ej: "CREAR_EVENTO", "CANCELAR_ORDEN"

    @Column(name = "modulo")
    private String modulo; // Ej: "EventoService", "OrdenServicio"

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle; // Ej: "Se creó el evento con ID: 123"

    @Column(name = "ip_usuario")
    private String ipUsuario; // Opcional pero recomendado

    // Importante: Quién hizo la acción
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin")
    private Administrador administrador;
}