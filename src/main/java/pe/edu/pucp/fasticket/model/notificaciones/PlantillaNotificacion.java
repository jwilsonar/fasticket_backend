package pe.edu.pucp.fasticket.model.notificaciones;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plantillas_notificacion")
@Getter
@Setter
public class PlantillaNotificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, unique = true, length = 64)
	private TipoPlantilla tipo;

	@Column(name = "asunto", nullable = false, length = 255)
	private String asunto;

	@Column(name = "html", nullable = false, columnDefinition = "TEXT")
	private String html;

	@Column(name = "habilitado", nullable = false)
	private boolean habilitado = true;

	@Column(name = "actualizado_en", nullable = false)
	private LocalDateTime actualizadoEn = LocalDateTime.now();
}


