package pe.edu.pucp.fasticket.services.soporte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarEstadoSolicitudDTO;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.CrearSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.SolicitudSoporteResponseDTO;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;
import pe.edu.pucp.fasticket.model.soporte.SolicitudSoporte;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.soporte.SolicitudSoporteRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SolicitudSoporteServiceImpl implements SolicitudSoporteService {

    private final SolicitudSoporteRepositorio soporteRepositorio;
    private final PersonasRepositorio personasRepositorio;

    @Override
    public SolicitudSoporteResponseDTO crear(CrearSolicitudSoporteDTO dto, UserDetails userDetails) {
        Persona solicitante = resolvePersona(dto.getIdUsuario(), userDetails);
        Integer usuarioAuditoria = obtenerIdAuditoria(userDetails);

        SolicitudSoporte solicitud = SolicitudSoporte.builder()
                .usuario(solicitante)
                .asunto(dto.getAsunto())
                .mensaje(dto.getMensaje())
                .prioridad(dto.getPrioridad())
                .estado(EstadoSoporte.ABIERTO)
                .canalOrigen(dto.getCanalOrigen())
                .ipOrigen(dto.getIpOrigen())
                .metadataAdicional(dto.getMetadataAdicional())
                .fechaCreacion(LocalDateTime.now())
                .usuarioCreacion(usuarioAuditoria != null ? usuarioAuditoria : (solicitante != null ? solicitante.getIdPersona() : null))
                .activo(true)
                .build();

        SolicitudSoporte guardada = soporteRepositorio.save(solicitud);
        log.info("Ticket de soporte {} creado", guardada.getIdSolicitud());
        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudSoporteResponseDTO> listar(Integer idUsuario, EstadoSoporte estado) {
        List<SolicitudSoporte> registros;

        if (idUsuario != null && estado != null) {
            registros = soporteRepositorio.findByUsuarioIdPersonaAndEstado(idUsuario, estado);
        } else if (idUsuario != null) {
            registros = soporteRepositorio.findByUsuarioIdPersona(idUsuario);
        } else if (estado != null) {
            registros = soporteRepositorio.findByEstado(estado);
        } else {
            registros = soporteRepositorio.findByActivoTrue();
        }

        return registros.stream()
                .filter(solicitud -> Boolean.TRUE.equals(solicitud.getActivo()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudSoporteResponseDTO obtenerPorId(Long idSolicitud) {
        SolicitudSoporte solicitud = soporteRepositorio.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de soporte no encontrada con ID: " + idSolicitud));
        return toResponse(solicitud);
    }

    @Override
    public SolicitudSoporteResponseDTO actualizar(Long idSolicitud, ActualizarSolicitudSoporteDTO dto, UserDetails userDetails) {
        SolicitudSoporte solicitud = soporteRepositorio.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de soporte no encontrada con ID: " + idSolicitud));

        if (dto.getAsunto() != null) {
            solicitud.setAsunto(dto.getAsunto());
        }
        if (dto.getMensaje() != null) {
            solicitud.setMensaje(dto.getMensaje());
        }
        if (dto.getPrioridad() != null) {
            solicitud.setPrioridad(dto.getPrioridad());
        }
        if (dto.getCanalOrigen() != null) {
            solicitud.setCanalOrigen(dto.getCanalOrigen());
        }
        if (dto.getIpOrigen() != null) {
            solicitud.setIpOrigen(dto.getIpOrigen());
        }
        if (dto.getMetadataAdicional() != null) {
            solicitud.setMetadataAdicional(dto.getMetadataAdicional());
        }
        if (dto.getObservaciones() != null) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        solicitud.setUsuarioActualizacion(obtenerIdAuditoria(userDetails));
        solicitud.setFechaActualizacion(LocalDateTime.now());

        SolicitudSoporte actualizada = soporteRepositorio.save(solicitud);
        log.info("Ticket de soporte {} actualizado", idSolicitud);
        return toResponse(actualizada);
    }

    @Override
    public SolicitudSoporteResponseDTO actualizarEstado(Long idSolicitud, ActualizarEstadoSolicitudDTO dto, UserDetails userDetails) {
        SolicitudSoporte solicitud = soporteRepositorio.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de soporte no encontrada con ID: " + idSolicitud));

        solicitud.setEstado(dto.getEstado());
        if (dto.getObservaciones() != null) {
            solicitud.setObservaciones(dto.getObservaciones());
        }

        if (EstadoSoporte.RESUELTO.equals(dto.getEstado()) || EstadoSoporte.CERRADO.equals(dto.getEstado())) {
            solicitud.setFechaCierre(LocalDateTime.now());
        }

        solicitud.setUsuarioActualizacion(obtenerIdAuditoria(userDetails));
        solicitud.setFechaActualizacion(LocalDateTime.now());

        SolicitudSoporte actualizada = soporteRepositorio.save(solicitud);
        log.info("Ticket de soporte {} pasó al estado {}", idSolicitud, dto.getEstado());
        return toResponse(actualizada);
    }

    @Override
    public void eliminar(Long idSolicitud, UserDetails userDetails) {
        SolicitudSoporte solicitud = soporteRepositorio.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de soporte no encontrada con ID: " + idSolicitud));

        solicitud.setActivo(false);
        solicitud.setUsuarioActualizacion(obtenerIdAuditoria(userDetails));
        solicitud.setFechaActualizacion(LocalDateTime.now());
        soporteRepositorio.save(solicitud);

        log.info("Ticket de soporte {} marcado como inactivo", idSolicitud);
    }

    private SolicitudSoporteResponseDTO toResponse(SolicitudSoporte solicitud) {
        Persona persona = solicitud.getUsuario();
        return SolicitudSoporteResponseDTO.builder()
                .idSolicitud(solicitud.getIdSolicitud())
                .idUsuario(persona != null ? persona.getIdPersona() : null)
                .nombreUsuario(persona != null ? persona.getNombres() + " " + persona.getApellidos() : null)
                .emailUsuario(persona != null ? persona.getEmail() : null)
                .asunto(solicitud.getAsunto())
                .mensaje(solicitud.getMensaje())
                .estado(solicitud.getEstado())
                .prioridad(solicitud.getPrioridad())
                .canalOrigen(solicitud.getCanalOrigen())
                .ipOrigen(solicitud.getIpOrigen())
                .metadataAdicional(solicitud.getMetadataAdicional())
                .observaciones(solicitud.getObservaciones())
                .fechaCreacion(solicitud.getFechaCreacion())
                .fechaActualizacion(solicitud.getFechaActualizacion())
                .fechaCierre(solicitud.getFechaCierre())
                .build();
    }

    private Persona resolvePersona(Integer idUsuario, UserDetails userDetails) {
        if (idUsuario != null) {
            return personasRepositorio.findByIdPersona(idUsuario)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con ID: " + idUsuario));
        }
        if (userDetails != null) {
            return personasRepositorio.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    private Integer obtenerIdAuditoria(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return personasRepositorio.findByEmail(userDetails.getUsername())
                .map(Persona::getIdPersona)
                .orElse(null);
    }
}

