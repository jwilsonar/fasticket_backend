package pe.edu.pucp.fasticket.repository.soporte;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;
import pe.edu.pucp.fasticket.model.soporte.SolicitudSoporte;

@Repository
public interface SolicitudSoporteRepositorio extends JpaRepository<SolicitudSoporte, Long> {

    List<SolicitudSoporte> findByActivoTrue();

    List<SolicitudSoporte> findByUsuarioIdPersona(Integer idPersona);

    List<SolicitudSoporte> findByUsuarioIdPersonaAndEstado(Integer idPersona, EstadoSoporte estado);

    List<SolicitudSoporte> findByEstado(EstadoSoporte estado);
}

