package pe.edu.pucp.fasticket.services.soporte;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

import pe.edu.pucp.fasticket.dto.soporte.ActualizarEstadoSolicitudDTO;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.CrearSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.SolicitudSoporteResponseDTO;
import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;

public interface SolicitudSoporteService {

    SolicitudSoporteResponseDTO crear(CrearSolicitudSoporteDTO dto, UserDetails userDetails);

    List<SolicitudSoporteResponseDTO> listar(Integer idUsuario, EstadoSoporte estado);

    SolicitudSoporteResponseDTO obtenerPorId(Long idSolicitud);

    SolicitudSoporteResponseDTO actualizar(Long idSolicitud, ActualizarSolicitudSoporteDTO dto, UserDetails userDetails);

    SolicitudSoporteResponseDTO actualizarEstado(Long idSolicitud, ActualizarEstadoSolicitudDTO dto, UserDetails userDetails);

    void eliminar(Long idSolicitud, UserDetails userDetails);
}

