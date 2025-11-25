package pe.edu.pucp.fasticket.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.fasticket.dto.compra.CrearSolicitudTransferenciaDTO;
import pe.edu.pucp.fasticket.dto.compra.SolicitudTransferenciaDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.EstadoSolicitud;
import pe.edu.pucp.fasticket.model.compra.SolicitudTransferencia;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.compra.SolicitudTransferenciaRepository;
import pe.edu.pucp.fasticket.repository.compra.TransferenciaEntradaRepository;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.EmailService;
import pe.edu.pucp.fasticket.services.compra.TransferenciaEntradaServicio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaEntradaServicioTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private SolicitudTransferenciaRepository solicitudRepository;
    @Mock private ConfiguracionRepository configuracionRepository;
    @Mock private TransferenciaEntradaRepository historialRepository; // Necesario aunque no se use directo en estos tests
    @Mock private EmailService emailService;

    @InjectMocks
    private TransferenciaEntradaServicio servicio;

    private Ticket ticket;
    private Cliente emisor;
    private Cliente receptor;
    private ConfiguracionGlobal configLimite;
    private ConfiguracionGlobal configTiempo;

    @BeforeEach
    void setUp() {
        // Datos de prueba
        emisor = new Cliente();
        emisor.setIdPersona(1);
        emisor.setEmail("emisor@test.com");
        emisor.setNombres("Juan");

        receptor = new Cliente();
        receptor.setIdPersona(2);
        receptor.setEmail("receptor@test.com");
        receptor.setNombres("Pedro");

        ticket = new Ticket();
        ticket.setIdTicket(100);
        ticket.setCliente(emisor);
        ticket.setEstado(EstadoTicket.VENDIDA);
        ticket.setContadorTransferencias(0);
        ticket.setEvento(new Evento()); // Evitar NPE en mapper

        // Mock Configuración
        configLimite = new ConfiguracionGlobal();
        configLimite.setValue("2"); // Límite de 2

        configTiempo = new ConfiguracionGlobal();
        configTiempo.setValue("48");
    }

    @Test
    void crearSolicitud_Exito() {
        // GIVEN
        CrearSolicitudTransferenciaDTO dto = new CrearSolicitudTransferenciaDTO();
        dto.setIdTicket(100);
        dto.setEmailReceptor("receptor@test.com");

        when(ticketRepository.findById(100)).thenReturn(Optional.of(ticket));
        when(configuracionRepository.findById("LIMITE_TRANSFERENCIAS_TICKET")).thenReturn(Optional.of(configLimite));
        when(clienteRepository.findByEmail("receptor@test.com")).thenReturn(Optional.of(receptor));
        when(configuracionRepository.findById("TIEMPO_EXPIRACION_SOLICITUD_HORAS")).thenReturn(Optional.of(configTiempo));
        when(solicitudRepository.save(any(SolicitudTransferencia.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        SolicitudTransferenciaDTO resultado = servicio.crearSolicitudTransferencia(1, dto);

        // THEN
        assertNotNull(resultado);
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.getEstado());
        assertEquals("receptor@test.com", resultado.getEmailReceptor());
        verify(solicitudRepository).save(any(SolicitudTransferencia.class));
    }

    @Test
    void crearSolicitud_Falla_SiNoEsDueño() {
        // GIVEN: El emisor intenta transferir, pero el ID no coincide
        when(ticketRepository.findById(100)).thenReturn(Optional.of(ticket));

        CrearSolicitudTransferenciaDTO dto = new CrearSolicitudTransferenciaDTO();
        dto.setIdTicket(100);

        // WHEN & THEN
        assertThrows(BusinessException.class, () -> {
            servicio.crearSolicitudTransferencia(99, dto); // ID 99 no es el dueño (1)
        });
    }

    @Test
    void crearSolicitud_Falla_LimiteAlcanzado() {
        // GIVEN: Ticket ya transferido 2 veces
        ticket.setContadorTransferencias(2);

        CrearSolicitudTransferenciaDTO dto = new CrearSolicitudTransferenciaDTO();
        dto.setIdTicket(100);

        when(ticketRepository.findById(100)).thenReturn(Optional.of(ticket));
        when(configuracionRepository.findById("LIMITE_TRANSFERENCIAS_TICKET")).thenReturn(Optional.of(configLimite));

        // WHEN & THEN
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            servicio.crearSolicitudTransferencia(1, dto);
        });
        assertTrue(ex.getMessage().contains("Límite de transferencias alcanzado"));
    }
}