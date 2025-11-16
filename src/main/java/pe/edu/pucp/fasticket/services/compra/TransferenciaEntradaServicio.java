package pe.edu.pucp.fasticket.services.compra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.compra.*;
import pe.edu.pucp.fasticket.events.TicketTransferidoEvent;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.EstadoSolicitud;
import pe.edu.pucp.fasticket.model.compra.SolicitudTransferencia;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.SolicitudTransferenciaRepository;
import pe.edu.pucp.fasticket.repository.compra.TransferenciaRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferenciaEntradaServicio {

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final TransferenciaRepositorio transferenciaRepositorio;
    private final SolicitudTransferenciaRepository solicitudRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final Integer HORAS_EXPIRACION_SOLICITUD = 48;

    @Transactional
    public SolicitudTransferenciaDTO crearSolicitudTransferencia(
            Integer idEmisor, CrearSolicitudTransferenciaDTO dto) {
        log.info("Creando solicitud de transferencia de ticket {} de emisor {} a email {}",
                dto.getIdTicket(), idEmisor, dto.getEmailReceptor());
        Ticket ticket = ticketRepository.findById(dto.getIdTicket())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
        if (!ticket.getCliente().getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No eres el propietario de este ticket.");
        }
        if (ticket.getEstado() != EstadoTicket.VENDIDA && ticket.getEstado() != EstadoTicket.TRANSFERIDA) {
            throw new BusinessException("Solo puedes transferir tickets VENDIDOS o TRANSFERIDOS.");
        }
        Cliente receptor = clienteRepository.findByEmail(dto.getEmailReceptor())
                .orElseThrow(() -> new BusinessException(
                        "El email '" + dto.getEmailReceptor() + "' no está registrado. " +
                                "El destinatario debe crear una cuenta."));
        if (receptor.getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No puedes transferir un ticket a ti mismo.");
        }
        String nombreCompletoEntidad = receptor.getNombres() + " " + receptor.getApellidos();
        boolean nombreCoincide = nombreCompletoEntidad.equalsIgnoreCase(dto.getNombreCompletoReceptor());
        boolean docCoincide = receptor.getDocIdentidad().equals(dto.getNumeroDocumentoReceptor());
        boolean telCoincide = receptor.getTelefono().equals(dto.getTelefonoReceptor());

        if (!nombreCoincide || !docCoincide || !telCoincide) {
            throw new BusinessException("Los datos (Nombre, Documento o Teléfono) no coinciden con el email registrado.");
        }

        Evento evento = ticket.getEvento();
        Integer maxTransf = evento.getMaxTransferenciasPermitidas();
        Integer contActual = ticket.getContadorTransferencias();

        if (contActual >= maxTransf) {
            throw new BusinessException("Este ticket ya alcanzó el límite de " + maxTransf + " transferencias.");
        }

        Integer cooldownHoras = evento.getHorasCooldownTransferencia();
        if (ticket.getFechaUltimaTransferencia() != null) {
            LocalDateTime finCooldown = ticket.getFechaUltimaTransferencia().plusHours(cooldownHoras);

            if (LocalDateTime.now().isBefore(finCooldown)) {
                long horasRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), finCooldown);
                long minutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), finCooldown) % 60;

                throw new BusinessException(
                        String.format("Debes esperar el período de enfriamiento. Tiempo restante: %d horas y %d minutos.",
                                horasRestantes, minutosRestantes));
            }
        }

        solicitudRepository.findByTicket_IdTicketAndReceptor_IdPersonaAndEstadoAndActivoTrue(
                        ticket.getIdTicket(), receptor.getIdPersona(), EstadoSolicitud.PENDIENTE)
                .ifPresent(s -> {
                    if (s.getFechaExpiracion() != null &&
                            LocalDateTime.now().isAfter(s.getFechaExpiracion())) {
                        s.setEstado(EstadoSolicitud.EXPIRADA);
                        s.setFechaRespuesta(LocalDateTime.now());
                        solicitudRepository.save(s);
                    } else {
                        throw new BusinessException("Ya existe una solicitud pendiente para este ticket y receptor.");
                    }
                });

        Cliente emisor = clienteRepository.findById(idEmisor).orElseThrow();

        SolicitudTransferencia solicitud = new SolicitudTransferencia();
        solicitud.setTicket(ticket);
        solicitud.setEmisor(emisor);
        solicitud.setReceptor(receptor);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setFechaExpiracion(LocalDateTime.now().plusHours(HORAS_EXPIRACION_SOLICITUD));
        solicitud.setActivo(true);

        SolicitudTransferencia solicitudGuardada = solicitudRepository.save(solicitud);
        log.info("Solicitud de transferencia ID {} creada", solicitudGuardada.getIdSolicitud());

        return convertirSolicitudADTO(solicitudGuardada);
    }

    @Transactional(readOnly = true)
    public List<SolicitudTransferenciaDTO> obtenerSolicitudesPendientes(Integer idReceptor) {
        List<SolicitudTransferencia> solicitudes = solicitudRepository
                .findByReceptor_IdPersonaAndEstadoAndActivoTrue(idReceptor, EstadoSolicitud.PENDIENTE);

        return solicitudes.stream()
                .filter(s -> !estaExpirada(s))
                .map(this::convertirSolicitudADTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar todas las solicitudes enviadas por un emisor
     */
    @Transactional(readOnly = true)
    public List<SolicitudTransferenciaDTO> obtenerSolicitudesEnviadas(Integer idEmisor) {
        List<SolicitudTransferencia> solicitudes = solicitudRepository
                .findByEmisor_IdPersonaAndActivoTrueOrderByFechaSolicitudDesc(idEmisor);

        return solicitudes.stream()
                .map(this::convertirSolicitudADTO)
                .collect(Collectors.toList());
    }

    /**
     * Responder a una solicitud (aceptar o rechazar)
     */
    @Transactional
    public SolicitudTransferenciaDTO responderSolicitud(
            Integer idReceptor, ResponderSolicitudDTO dto) {

        log.info("Receptor {} respondiendo solicitud {}: {}",
                idReceptor, dto.getIdSolicitud(), dto.getAceptar() ? "ACEPTAR" : "RECHAZAR");

        SolicitudTransferencia solicitud = solicitudRepository.findById(dto.getIdSolicitud())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getIdPersona().equals(idReceptor)) {
            throw new BusinessException("No eres el destinatario de esta solicitud.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Esta solicitud ya fue respondida o expiró.");
        }

        // ⏰ VERIFICAR SI EXPIRÓ
        if (estaExpirada(solicitud)) {
            solicitud.setEstado(EstadoSolicitud.EXPIRADA);
            solicitud.setFechaRespuesta(LocalDateTime.now());
            solicitudRepository.save(solicitud);
            throw new BusinessException("Esta solicitud ha expirado.");
        }

        solicitud.setFechaRespuesta(LocalDateTime.now());

        if (dto.getAceptar()) {
            solicitud.setEstado(EstadoSolicitud.ACEPTADA);
            solicitudRepository.save(solicitud);

            ejecutarTransferenciaDesdeAceptacion(solicitud);

            log.info("✅ Solicitud {} ACEPTADA. Transferencia ejecutada.", solicitud.getIdSolicitud());

        } else {
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitudRepository.save(solicitud);

            log.info("❌ Solicitud {} RECHAZADA.", solicitud.getIdSolicitud());
        }

        return convertirSolicitudADTO(solicitud);
    }

    /**
     * Cancelar una solicitud (solo el emisor)
     */
    @Transactional
    public void cancelarSolicitud(Integer idEmisor, Integer idSolicitud) {
        SolicitudTransferencia solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!solicitud.getEmisor().getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No eres el emisor de esta solicitud.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo puedes cancelar solicitudes pendientes.");
        }

        if (estaExpirada(solicitud)) {
            solicitud.setEstado(EstadoSolicitud.EXPIRADA);
            solicitud.setFechaRespuesta(LocalDateTime.now());
            solicitudRepository.save(solicitud);
            throw new BusinessException("Esta solicitud ya expiró.");
        }

        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        log.info("🚫 Solicitud {} CANCELADA por el emisor", idSolicitud);
    }

    /**
     * ⏰ VERIFICAR SI EXPIRÓ (TIEMPO REAL)
     */
    private boolean estaExpirada(SolicitudTransferencia solicitud) {
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            return false;
        }

        if (solicitud.getFechaExpiracion() == null) {
            return false;
        }

        return LocalDateTime.now().isAfter(solicitud.getFechaExpiracion());
    }

    /**
     * Ejecutar la transferencia cuando se acepta
     */
    private void ejecutarTransferenciaDesdeAceptacion(SolicitudTransferencia solicitud) {
        Ticket ticket = solicitud.getTicket();
        Cliente emisor = solicitud.getEmisor();
        Cliente receptor = solicitud.getReceptor();

        ticket.setCliente(receptor);
        ticket.setEstado(EstadoTicket.TRANSFERIDA);
        ticket.setContadorTransferencias(ticket.getContadorTransferencias() + 1);
        ticket.setFechaUltimaTransferencia(LocalDateTime.now());
        ticketRepository.save(ticket);

        TransferenciaEntrada historial = new TransferenciaEntrada();
        historial.setTicket(ticket);
        historial.setEmisor(emisor);
        historial.setReceptor(receptor);
        historial.setFechaTransferencia(LocalDateTime.now());
        TransferenciaEntrada historialGuardado = transferenciaRepositorio.save(historial);

        log.info("✅ Transferencia registrada en historial ID {}", historialGuardado.getIdTransferencia());

        eventPublisher.publishEvent(new TicketTransferidoEvent(historialGuardado));
    }

    // ==================== HISTORIAL ====================

    @Transactional(readOnly = true)
    public List<TransferenciaResponseDTO> verHistorialDeTicket(Integer idTicket) {
        List<TransferenciaEntrada> historial = transferenciaRepositorio
                .findByTicket_IdTicketOrderByFechaTransferenciaDesc(idTicket);
        return historial.stream()
                .map(this::convertirHistorialADTO)
                .collect(Collectors.toList());
    }

    // ==================== CONVERSORES DTO ====================

    private SolicitudTransferenciaDTO convertirSolicitudADTO(SolicitudTransferencia s) {
        SolicitudTransferenciaDTO dto = new SolicitudTransferenciaDTO();
        dto.setIdSolicitud(s.getIdSolicitud());
        dto.setIdTicket(s.getTicket().getIdTicket());
        dto.setCodigoTicket(s.getTicket().getCodigoQr());
        dto.setNombreEvento(s.getTicket().getEvento().getNombre());
        dto.setFechaEvento(s.getTicket().getEvento().getFechaEvento());

        dto.setIdEmisor(s.getEmisor().getIdPersona());
        dto.setNombreEmisor(s.getEmisor().getNombres() + " " + s.getEmisor().getApellidos());
        dto.setEmailEmisor(s.getEmisor().getEmail());

        dto.setIdReceptor(s.getReceptor().getIdPersona());
        dto.setNombreReceptor(s.getReceptor().getNombres() + " " + s.getReceptor().getApellidos());
        dto.setEmailReceptor(s.getReceptor().getEmail());

        dto.setEstado(s.getEstado());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaRespuesta(s.getFechaRespuesta());
        dto.setFechaExpiracion(s.getFechaExpiracion());

        // ⏰ Calcular horas restantes
        if (s.getEstado() == EstadoSolicitud.PENDIENTE && s.getFechaExpiracion() != null) {
            if (LocalDateTime.now().isBefore(s.getFechaExpiracion())) {
                long horas = ChronoUnit.HOURS.between(LocalDateTime.now(), s.getFechaExpiracion());
                dto.setHorasRestantes(horas);
            } else {
                dto.setHorasRestantes(0L);
            }
        }

        Evento evento = s.getTicket().getEvento();
        dto.setTransferenciasRestantes(
                evento.getMaxTransferenciasPermitidas() - s.getTicket().getContadorTransferencias());

        // Validar si puede transferir (cooldown)
        Ticket ticket = s.getTicket();
        boolean puedeTransferir = true;
        if (ticket.getFechaUltimaTransferencia() != null) {
            LocalDateTime finCooldown = ticket.getFechaUltimaTransferencia()
                    .plusHours(evento.getHorasCooldownTransferencia());
            puedeTransferir = LocalDateTime.now().isAfter(finCooldown);
        }
        dto.setPuedeTransferir(puedeTransferir);

        return dto;
    }

    private TransferenciaResponseDTO convertirHistorialADTO(TransferenciaEntrada h) {
        TransferenciaResponseDTO dto = new TransferenciaResponseDTO();
        dto.setIdHistorial(h.getIdTransferencia());
        dto.setIdTicket(h.getTicket().getIdTicket());
        dto.setNombreEmisor(h.getEmisor().getNombres() + " " + h.getEmisor().getApellidos());
        dto.setEmailEmisor(h.getEmisor().getEmail());
        dto.setNombreReceptor(h.getReceptor().getNombres() + " " + h.getReceptor().getApellidos());
        dto.setEmailReceptor(h.getReceptor().getEmail());
        dto.setFechaTransferencia(h.getFechaTransferencia());
        return dto;
    }
}
