package pe.edu.pucp.fasticket.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.ValidacionResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ValidacionService {

    private final TicketRepository ticketRepository;

    /**
     * Valida un ticket por su código QR y lo marca como CANJEADO.
     * RF-094
     */
    public ValidacionResponseDTO validarTicket(String codigoQr) {
        log.info("Validando ticket con QR: {}", codigoQr);

        // 1. Buscar el ticket
        Ticket ticket = ticketRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET NO ENCONTRADO"));

        // 2. Revisar el estado del ticket
        if (ticket.getEstado() == EstadoTicket.CANJEADA) {
            log.warn("Intento de doble canje para ticket ID: {}", ticket.getIdTicket());
            throw new BusinessException("TICKET YA FUE USADO");
        }

        // Usamos los estados que SÍ tienes
        if (ticket.getEstado() != EstadoTicket.VENDIDA && ticket.getEstado() != EstadoTicket.TRANSFERIDA) {
            log.warn("Intento de canje de ticket no válido. Estado: {}", ticket.getEstado());
            // Si está ANULADA, RESERVADA o DISPONIBLE, falla.
            throw new BusinessException("TICKET NO VÁLIDO (No fue vendido, está anulado o pendiente)");
        }

        // 3. ¡Canjear el ticket!
        ticket.setEstado(EstadoTicket.CANJEADA);
        ticket.setFechaActualizacion(java.time.LocalDate.now());
        ticketRepository.save(ticket);

        log.info("Ticket ID: {} canjeado exitosamente.", ticket.getIdTicket());

        // 4. Devolver respuesta exitosa
        return new ValidacionResponseDTO(
                "ACCESO PERMITIDO",
                ticket.getEvento().getNombre(),
                ticket.getTipoTicket().getNombre(),
                ticket.getNombreAsistente() + " " + ticket.getApellidoAsistente(),
                ticket.getDocumentoAsistente()
        );
    }
}