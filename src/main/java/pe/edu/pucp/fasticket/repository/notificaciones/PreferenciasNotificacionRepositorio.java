package pe.edu.pucp.fasticket.repository.notificaciones;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.notificaciones.PreferenciasNotificacion;

@Repository
public interface PreferenciasNotificacionRepositorio extends JpaRepository<PreferenciasNotificacion, Integer> {
	Optional<PreferenciasNotificacion> findById(Integer personaId);
}


