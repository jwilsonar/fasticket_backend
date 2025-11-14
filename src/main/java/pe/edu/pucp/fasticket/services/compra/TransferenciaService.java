package pe.edu.pucp.fasticket.services.compra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.compra.TransferenciaRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.TransferenciaEntrada;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.TransferenciaEntradaRepository; // Asumo que existe
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.EmailService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransferenciaService {

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final TransferenciaEntradaRepository transferenciaRepository;
    private final EmailService emailService;

    /**
     * Lógica para RF-092 y RF-093: Transferir un ticket
     */
    public void transferirTicket(Integer idTicket, String emailEmisor, TransferenciaRequestDTO request) {
        log.info("Iniciando transferencia de ticket ID: {} de {} para {}",
                idTicket, emailEmisor, request.getEmailReceptor());

        // 1. Buscar el Ticket
        Ticket ticket = ticketRepository.findById(idTicket)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con ID: " + idTicket));

        // 2. Buscar Emisor (Dueño actual)
        Cliente emisor = clienteRepository.findByEmail(emailEmisor)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente emisor no encontrado"));

        // 3. Buscar Receptor
        Cliente receptor = clienteRepository.findByEmail(request.getEmailReceptor())
                .orElseThrow(() -> new ResourceNotFoundException("El email del destinatario no corresponde a un cliente registrado."));

        // 4. Validaciones de Negocio
        if (emisor.getIdPersona().equals(receptor.getIdPersona())) {
            throw new BusinessException("No puedes transferirte un ticket a ti mismo.");
        }
        if (!ticket.getCliente().getIdPersona().equals(emisor.getIdPersona())) {
            throw new SecurityException("No tienes permiso para transferir este ticket.");
        }
        if (ticket.getEstado() != EstadoTicket.VENDIDA) {
            throw new BusinessException("Solo se pueden transferir tickets que hayan sido pagados (VENDIDA).");
        }
        // --- NUEVA VALIDACIÓN (Límite de 2) ---
        // Tu campo 'contadorTransferencias' se inicializa en 0.
        // 1ra transferencia: 0 < 2 (Pasa. Se incrementa a 1)
        // 2da transferencia: 1 < 2 (Pasa. Se incrementa a 2)
        // 3er intento:     2 < 2 (Falla. 2 no es menor que 2)
        if (ticket.getContadorTransferencias() >= 2) {//Este 2 se cambia por 1 en caso el límite de transferencias se cambia
            throw new BusinessException("Este ticket ya ha sido transferido 2 veces y no puede volver a transferirse.");
        }
        // --- FIN DE LA VALIDACIÓN ---

        // 5. ¡Realizar la Transferencia! (Modificar el Ticket)
        ticket.setCliente(receptor);
        ticket.setNombreAsistente(receptor.getNombres());
        ticket.setApellidoAsistente(receptor.getApellidos());
        ticket.setDocumentoAsistente(receptor.getDocIdentidad());
        ticket.setTipoDocumentoAsistente(receptor.getTipoDocumento());
        ticket.setEstado(EstadoTicket.TRANSFERIDA);
        ticket.setContadorTransferencias(ticket.getContadorTransferencias() + 1);
        ticket.setFechaUltimaTransferencia(LocalDateTime.now());
        ticket.setFechaActualizacion(java.time.LocalDate.now());

        ticketRepository.save(ticket);

        // 6. Guardar el registro de auditoría (RF-093)
        TransferenciaEntrada registro = new TransferenciaEntrada();
        registro.setTicket(ticket);
        registro.setEmisor(emisor);
        registro.setReceptor(receptor);
        registro.setFechaTransferencia(LocalDateTime.now());

        transferenciaRepository.save(registro);

        log.info("Transferencia completada. Ticket ID: {} ahora pertenece a {}", idTicket, receptor.getEmail());

        // 7. Notificar por Correo
        try {
            emailService.enviarCorreoTransferencia(emisor, receptor, ticket);
        } catch (Exception e) {
            log.warn("La transferencia fue exitosa, pero falló el envío de correo de notificación. Causa: {}", e.getMessage());
            // No detenemos la transacción si el email falla
        }
    }
}