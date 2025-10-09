package pe.edu.pucp.fasticket.model.auditoria;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "auditoria_admin")
public class AuditoriaAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false)
    private TipoAccion accion;

    @Column(name = "modulo")
    private String modulo;

    @Column(name = "fecha", columnDefinition = "TIMESTAMP")
    private LocalDateTime fecha;

    @Column(name = "ip_origen")
    private String ipOrigen;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "navegador")
    private String navegador;

    @Column(name = "sistema_operativo")
    private String sistemaOperativo;

    @Column(name = "activo")
    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario", nullable = false)
    private Administrador administrador;
}