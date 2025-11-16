package pe.edu.pucp.fasticket.model.notificaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "preferencias_notificacion")
@Getter
@Setter
public class PreferenciasNotificacion {

	@Id
	@Column(name = "persona_id")
	private Integer personaId;

	@Column(name = "habilitado", nullable = false)
	private boolean habilitado = true;
}


