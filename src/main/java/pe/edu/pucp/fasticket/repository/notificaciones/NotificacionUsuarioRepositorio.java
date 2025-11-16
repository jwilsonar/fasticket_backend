package pe.edu.pucp.fasticket.repository.notificaciones;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.notificaciones.NotificacionUsuario;

@Repository
public interface NotificacionUsuarioRepositorio extends JpaRepository<NotificacionUsuario, Long> {
	Page<NotificacionUsuario> findByPersonaIdOrderByCreadaEnDesc(Integer personaId, Pageable pageable);
	Page<NotificacionUsuario> findByPersonaIdAndLeidaOrderByCreadaEnDesc(Integer personaId, boolean leida, Pageable pageable);
}


