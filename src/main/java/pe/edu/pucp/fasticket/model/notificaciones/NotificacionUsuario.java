package pe.edu.pucp.fasticket.model.notificaciones;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notificaciones_usuario", indexes = {
	@Index(name = "idx_notif_usuario_persona", columnList = "persona_id"),
	@Index(name = "idx_notif_usuario_leida", columnList = "leida"),
	@Index(name = "idx_notif_usuario_fecha", columnList = "creada_en")
})
@Getter
@Setter
public class NotificacionUsuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "persona_id", nullable = false)
	private Integer personaId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 64)
	private TipoNotificacion tipo;

	@Column(name = "titulo", nullable = false, length = 200)
	private String titulo;

	@Lob
	@Column(name = "mensaje", nullable = false)
	private String mensaje;

	@Column(name = "leida", nullable = false)
	private boolean leida = false;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn = Instant.now();

	@Column(name = "leida_en")
	private Instant leidaEn;

	@Lob
	@Column(name = "metadata_json")
	private String metadataJson;
}


