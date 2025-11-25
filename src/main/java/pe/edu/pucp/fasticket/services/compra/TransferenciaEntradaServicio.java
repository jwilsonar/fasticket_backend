package pe.edu.pucp.fasticket.services.compra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.compra.*;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.EstadoSolicitud;
import pe.edu.pucp.fasticket.model.compra.SolicitudTransferencia;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.compra.SolicitudTransferenciaRepository;
import pe.edu.pucp.fasticket.repository.compra.TransferenciaEntradaRepository;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.EmailService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransferenciaEntradaServicio {

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final SolicitudTransferenciaRepository solicitudRepository;
    private final TransferenciaEntradaRepository historialRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final EmailService emailService;

    // --- 1. CREAR SOLICITUD ---
    public SolicitudTransferenciaDTO crearSolicitudTransferencia(Integer idEmisor, CrearSolicitudTransferenciaDTO dto) {

        Ticket ticket = ticketRepository.findById(dto.getIdTicket())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (!ticket.getCliente().getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No eres el propietario de este ticket.");
        }
        if (ticket.getEstado() != EstadoTicket.VENDIDA) {
            throw new BusinessException("El ticket no está en un estado válido para transferir.");
        }

        // Validación Configuración Dinámica
        int limiteMax = Integer.parseInt(
                configuracionRepository.findById("LIMITE_TRANSFERENCIAS_TICKET")
                        .map(ConfiguracionGlobal::getValue).orElse("1")
        );

        if (ticket.getContadorTransferencias() >= limiteMax) {
            throw new BusinessException("Límite de transferencias alcanzado (" + limiteMax + ").");
        }

        // Verificar pendientes (Usando el repo que me pasaste)
        boolean existePendiente = !solicitudRepository.findByTicket_IdTicketAndEstadoAndActivoTrue(
                ticket.getIdTicket(), EstadoSolicitud.PENDIENTE).isEmpty();

        if (existePendiente) {
            throw new BusinessException("Ya existe una solicitud pendiente para este ticket.");
        }

        Cliente receptor = clienteRepository.findByEmail(dto.getEmailReceptor())
                .orElseThrow(() -> new BusinessException("El correo destinatario no existe."));

        if (receptor.getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No puedes transferirte a ti mismo.");
        }

        SolicitudTransferencia solicitud = new SolicitudTransferencia();
        solicitud.setTicket(ticket);
        solicitud.setEmisor(ticket.getCliente());
        solicitud.setReceptor(receptor);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now()); // NOMBRE CORREGIDO
        solicitud.setActivo(true);

        int horasExp = Integer.parseInt(
                configuracionRepository.findById("TIEMPO_EXPIRACION_SOLICITUD_HORAS")
                        .map(ConfiguracionGlobal::getValue).orElse("48")
        );
        solicitud.setFechaExpiracion(LocalDateTime.now().plusHours(horasExp)); // NOMBRE CORREGIDO

        solicitudRepository.save(solicitud);
        return mapToDTO(solicitud);
    }

    // --- 2. LISTAR ---
    public List<SolicitudTransferenciaDTO> obtenerSolicitudesPendientes(Integer idReceptor) {
        return solicitudRepository.findByReceptor_IdPersonaAndEstadoAndActivoTrue(idReceptor, EstadoSolicitud.PENDIENTE)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<SolicitudTransferenciaDTO> obtenerSolicitudesEnviadas(Integer idEmisor) {
        return solicitudRepository.findByEmisor_IdPersonaAndActivoTrueOrderByFechaSolicitudDesc(idEmisor)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // --- 3. RESPONDER ---
    public SolicitudTransferenciaDTO responderSolicitud(Integer idReceptor, ResponderSolicitudDTO dto) {
        SolicitudTransferencia solicitud = solicitudRepository.findById(dto.getIdSolicitud())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getIdPersona().equals(idReceptor)) {
            throw new BusinessException("No autorizado.");
        }

        // Validación manual de vencimiento si el estado sigue PENDIENTE pero la fecha pasó
        if (solicitud.getEstado() == EstadoSolicitud.PENDIENTE && LocalDateTime.now().isAfter(solicitud.getFechaExpiracion())) {
            solicitud.setEstado(EstadoSolicitud.VENCIDO);
            solicitudRepository.save(solicitud);
            throw new BusinessException("La solicitud ha expirado.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solicitud ya procesada o vencida.");
        }

        solicitud.setFechaRespuesta(LocalDateTime.now());

        if (dto.getAceptar()) {
            ejecutarTransferencia(solicitud);
            solicitud.setEstado(EstadoSolicitud.ACEPTADO);
        } else {
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
        }

        return mapToDTO(solicitudRepository.save(solicitud));
    }

    // --- 4. CANCELAR ---
    public void cancelarSolicitud(Integer idEmisor, Integer idSolicitud) {
        SolicitudTransferencia solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!solicitud.getEmisor().getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No eres el emisor.");
        }
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo se cancelan solicitudes pendientes.");
        }

        solicitud.setEstado(EstadoSolicitud.CANCELADO);
        solicitudRepository.save(solicitud);
    }

    // --- LÓGICA INTERNA ---
    private void ejecutarTransferencia(SolicitudTransferencia solicitud) {
        Ticket ticket = solicitud.getTicket();
        Cliente nuevo = solicitud.getReceptor();
        Cliente antiguo = solicitud.getEmisor();

        ticket.setCliente(nuevo);
        ticket.setNombreAsistente(nuevo.getNombres());
        ticket.setApellidoAsistente(nuevo.getApellidos());
        ticket.setDocumentoAsistente(nuevo.getDocIdentidad());
        ticket.setTipoDocumentoAsistente(nuevo.getTipoDocumento());

        ticket.setContadorTransferencias(ticket.getContadorTransferencias() + 1);
        ticket.setFechaUltimaTransferencia(LocalDateTime.now());
        ticket.setCodigoQr(UUID.randomUUID().toString()); // Nuevo QR

        ticketRepository.save(ticket);

        // --- AQUÍ SE GUARDA EL HISTORIAL/AUDITORÍA (RF-093) ---
        TransferenciaEntrada historial = new TransferenciaEntrada();
        historial.setTicket(ticket);
        historial.setEmisor(antiguo);
        historial.setReceptor(nuevo);
        historial.setFechaTransferencia(LocalDateTime.now());

        historialRepository.save(historial);

        try {
            emailService.enviarCorreoTransferencia(antiguo, nuevo, ticket);
        } catch(Exception e) { log.error("Error email", e); }
    }

    // --- HISTORIAL ---
    public List<TransferenciaResponseDTO> verHistorialDeTicket(Integer idTicket) {
        // Debes tener un método en TransferenciaEntradaRepository que busque por ticket
        // return historialRepository.findByTicketId(idTicket)...
        return new ArrayList<>();
    }

    private SolicitudTransferenciaDTO mapToDTO(SolicitudTransferencia entity) {
        SolicitudTransferenciaDTO dto = new SolicitudTransferenciaDTO();

        // 1. Datos de la Solicitud
        dto.setIdSolicitud(entity.getIdSolicitud());
        dto.setEstado(entity.getEstado());
        dto.setFechaSolicitud(entity.getFechaSolicitud());
        dto.setFechaExpiracion(entity.getFechaExpiracion());
        dto.setFechaRespuesta(entity.getFechaRespuesta());

        // 2. Cálculos de Tiempo (Horas Restantes)
        if (entity.getEstado() == EstadoSolicitud.PENDIENTE && entity.getFechaExpiracion() != null) {
            long horas = java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), entity.getFechaExpiracion());
            dto.setHorasRestantes(horas > 0 ? horas : 0);
        } else {
            dto.setHorasRestantes(0L);
        }

        // 3. Datos del Ticket y Evento
        Ticket ticket = entity.getTicket();
        dto.setIdTicket(ticket.getIdTicket());
        dto.setCodigoTicket(ticket.getCodigoQr());

        if (ticket.getEvento() != null) {
            dto.setNombreEvento(ticket.getEvento().getNombre());
            dto.setFechaEvento(ticket.getEvento().getFechaEvento());
        }

        // 4. Datos de Emisor y Receptor
        Cliente emisor = entity.getEmisor();
        dto.setIdEmisor(emisor.getIdPersona());
        dto.setNombreEmisor(emisor.getNombres() + " " + emisor.getApellidos());
        dto.setEmailEmisor(emisor.getEmail());

        Cliente receptor = entity.getReceptor();
        dto.setIdReceptor(receptor.getIdPersona());
        dto.setNombreReceptor(receptor.getNombres() + " " + receptor.getApellidos());
        dto.setEmailReceptor(receptor.getEmail());

        // 5. Cálculos de Límites (Para saber si se puede volver a transferir)
        // Obtenemos el límite de la configuración (o 1 por defecto)
        int limiteMax = Integer.parseInt(
                configuracionRepository.findById("LIMITE_TRANSFERENCIAS_TICKET")
                        .map(ConfiguracionGlobal::getValue).orElse("1")
        );

        int transferenciasHechas = ticket.getContadorTransferencias() != null ? ticket.getContadorTransferencias() : 0;
        int restantes = limiteMax - transferenciasHechas;

        dto.setTransferenciasRestantes(restantes > 0 ? restantes : 0);
        dto.setPuedeTransferir(restantes > 0 && ticket.getEstado() == EstadoTicket.VENDIDA);

        return dto;
    }
}