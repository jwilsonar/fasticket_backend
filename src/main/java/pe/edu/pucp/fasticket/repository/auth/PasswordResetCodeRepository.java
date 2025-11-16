package pe.edu.pucp.fasticket.repository.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.auth.PasswordResetCode;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

	Optional<PasswordResetCode> findTopByEmailOrderByIdDesc(String email);

	Optional<PasswordResetCode> findTopByPersonaIdOrderByIdDesc(Integer personaId);

	@Modifying
	@Query("update PasswordResetCode p set p.usado = true where p.personaId = :personaId and p.verificado = true and p.usado = false")
	int marcarUsadosPorPersona(Integer personaId);

	@Modifying
	@Query("delete from PasswordResetCode p where p.expiraEn < :now")
	int eliminarExpirados(Instant now);
}

