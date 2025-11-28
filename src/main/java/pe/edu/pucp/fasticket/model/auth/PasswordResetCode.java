package pe.edu.pucp.fasticket.model.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "password_reset_codes", indexes = {
	@Index(name = "idx_prc_persona", columnList = "persona_id"),
	@Index(name = "idx_prc_email", columnList = "email")
})
@Getter
@Setter
public class PasswordResetCode {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "persona_id", nullable = true)
	private Integer personaId;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "codigo", nullable = false, length = 6)
	private String codigo;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@Column(name = "verificado", nullable = false)
	private boolean verificado = false;

	@Column(name = "usado", nullable = false)
	private boolean usado = false;

	@Column(name = "intentos", nullable = false)
	private int intentos = 0;
}


