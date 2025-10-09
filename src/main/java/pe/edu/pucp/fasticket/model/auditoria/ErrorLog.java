package pe.edu.pucp.fasticket.model.auditoria;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "error_log")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_error")
    private Integer idError;

    @Column(name = "fecha_hora", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaHora;

    @Column(name = "severidad")
    private String severidad;

    @Column(name = "modulo")
    private String modulo;

    @Column(name = "mensaje_breve")
    private String mensajeBreve;

    @Column(name = "detalle_tecnico", columnDefinition = "TEXT")
    private String detalleTecnico;

    @Column(name = "traza", columnDefinition = "TEXT")
    private String traza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin")
    private Administrador administrador;
}