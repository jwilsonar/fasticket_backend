package pe.edu.pucp.fasticket.services.compra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.compra.TransferenciaResponseDTO;
import pe.edu.pucp.fasticket.dto.compra.VerificarTransferenciaRequestDTO;
import pe.edu.pucp.fasticket.dto.compra.VerificarTransferenciaResponseDTO;
import pe.edu.pucp.fasticket.events.TicketTransferidoEvent;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.TransferenciaRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferenciaEntradaServicio {
    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final TransferenciaRepositorio transferenciaRepositorio;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public VerificarTransferenciaResponseDTO verificarTransferencia(
            Integer idEmisor, VerificarTransferenciaRequestDTO dto) {

        log.info("Verificando transferencia de ticket {} para emisor {}", dto.getIdTicket(), idEmisor);

        Ticket ticket = ticketRepository.findById(dto.getIdTicket())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        if (!ticket.getCliente().getIdPersona().equals(idEmisor)) {
            throw new BusinessException("No eres el propietario de este ticket.");
        }
        if (ticket.getEstado() != EstadoTicket.VENDIDA && ticket.getEstado() != EstadoTicket.TRANSFERIDA) {
            throw new BusinessException("Solo puedes transferir tickets que ya estén VENDIDOS o que ya hayan sido TRANSFERIDOS (re-transferencia).");
        }

        Cliente destinatario = clienteRepository.findByEmail(dto.getEmailDestinatario())
                .orElseThrow(() -> new BusinessException(
                        "El email '" + dto.getEmailDestinatario() + "' no está registrado. El destinatario debe crear una cuenta."
                ));

        String nombreCompletoEntidad = destinatario.getNombres() + " " + destinatario.getApellidos();
        boolean nombreCoincide = nombreCompletoEntidad.equalsIgnoreCase(dto.getNombreCompletoDestinatario());
        boolean docCoincide = destinatario.getDocIdentidad().equals(dto.getNumeroDocumentoDestinatario());
        boolean telCoincide = destinatario.getTelefono().equals(dto.getTelefonoDestinatario());

        if (!nombreCoincide || !docCoincide || !telCoincide) {
            throw new BusinessException("Los datos (Nombre, Documento o Teléfono) no coinciden con el email registrado.");
        }
        Evento evento = ticket.getTipoTicket().getEvento();

        Integer maxTransf = evento.getMaxTransferenciasPermitidas();
        Integer contActual = ticket.getContadorTransferencias();
        if (contActual >= maxTransf) {
            throw new BusinessException("Este ticket ya ha alcanzado el límite de " + maxTransf + " transferencias.");
        }

        Integer cooldownHoras = evento.getHorasCooldownTransferencia();
        if (ticket.getFechaUltimaTransferencia() != null &&
                ticket.getFechaUltimaTransferencia().plusHours(cooldownHoras).isAfter(LocalDateTime.now())) {

            throw new BusinessException("Debes esperar el período de enfriamiento de " + cooldownHoras + " horas.");
        }
        VerificarTransferenciaResponseDTO respuesta = new VerificarTransferenciaResponseDTO();
        respuesta.setNombreEvento(evento.getNombre());
        respuesta.setFechaEvento(evento.getFechaEvento());
        respuesta.setNombreDestinatario(nombreCompletoEntidad);
        respuesta.setEmailDestinatario(destinatario.getEmail());
        respuesta.setTransferenciasRestantes(maxTransf - contActual);
        respuesta.setHorasCooldown(cooldownHoras);

        return respuesta;
    }

    @Transactional
    public TransferenciaResponseDTO ejecutarTransferencia(Integer idEmisor, VerificarTransferenciaRequestDTO dto) {
        log.info("Ejecutando transferencia de ticket {} de emisor {} a email {}",
                dto.getIdTicket(), idEmisor, dto.getEmailDestinatario());
        verificarTransferencia(idEmisor, dto);
        Ticket ticket = ticketRepository.findById(dto.getIdTicket()).get();
        Cliente emisor = clienteRepository.findById(idEmisor).get();
        Cliente receptor = clienteRepository.findByEmail(dto.getEmailDestinatario()).get();
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
        log.info("Historial de transferencia ID {} creado.", historialGuardado.getIdTransferencia());
        eventPublisher.publishEvent(new TicketTransferidoEvent(historialGuardado));
        return convertirADTO(historialGuardado);
    }


    @Transactional(readOnly = true)
    public List<TransferenciaResponseDTO> verHistorialDeTicket(Integer idTicket) {
        List<TransferenciaEntrada> historial = transferenciaRepositorio.findByTicket_IdTicketOrderByFechaTransferenciaDesc(idTicket);
        return historial.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private TransferenciaResponseDTO convertirADTO(TransferenciaEntrada h) {
        TransferenciaResponseDTO dto = new TransferenciaResponseDTO();
        dto.setIdHistorial(h.getIdTransferencia());
        dto.setIdTicket(h.getTicket().getIdTicket());
        // Asumo que tienes getNombresCompletos() o similar en Cliente/Persona
        dto.setNombreEmisor(h.getEmisor().getNombres() + " " + h.getEmisor().getApellidos());
        dto.setEmailEmisor(h.getEmisor().getEmail());
        dto.setNombreReceptor(h.getReceptor().getNombres() + " " + h.getReceptor().getApellidos());
        dto.setEmailReceptor(h.getReceptor().getEmail());
        dto.setFechaTransferencia(h.getFechaTransferencia());
        return dto;
    }
}
