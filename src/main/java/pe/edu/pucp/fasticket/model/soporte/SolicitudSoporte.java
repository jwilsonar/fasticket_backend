package pe.edu.pucp.fasticket.model.soporte;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.usuario.Persona;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "solicitud_soporte")
public class SolicitudSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Persona usuario;

    @Column(name = "asunto", length = 150, nullable = false)
    private String asunto;

    @Column(name = "mensaje", columnDefinition = "TEXT", nullable = false)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    @Builder.Default
    private EstadoSoporte estado = EstadoSoporte.ABIERTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", length = 20, nullable = false)
    @Builder.Default
    private PrioridadSoporte prioridad = PrioridadSoporte.MEDIA;

    @Column(name = "canal_origen", length = 50)
    private String canalOrigen;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataAdicional;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "usuario_creacion")
    private Integer usuarioCreacion;

    @Column(name = "fecha_creacion", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_actualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fecha_actualizacion", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_cierre", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaCierre;
}

