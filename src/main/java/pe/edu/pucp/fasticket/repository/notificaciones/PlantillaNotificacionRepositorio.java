package pe.edu.pucp.fasticket.repository.notificaciones;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.notificaciones.PlantillaNotificacion;
import pe.edu.pucp.fasticket.model.notificaciones.TipoPlantilla;

@Repository
public interface PlantillaNotificacionRepositorio extends JpaRepository<PlantillaNotificacion, Long> {
	Optional<PlantillaNotificacion> findByTipoAndHabilitadoTrue(TipoPlantilla tipo);
}


