package pe.edu.pucp.fasticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemSeleccionadoDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenCompraRepositorio ordenCompraRepositorio;
    @Mock
    private TipoTicketRepository tipoTicketRepositorio;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    // --- Instancia del Servicio a probar ---
    @InjectMocks // Crea una instancia de OrdenServicio e inyecta los mocks
    private OrdenServicio ordenServicio;

    // --- Datos de prueba reutilizables ---
    private Cliente clienteMock;
    private Evento eventoMock;
    private TipoTicket tipoTicketMock;
    private CrearOrdenDTO crearOrdenDTO;
    private ItemSeleccionadoDTO itemSeleccionadoDTO;
    private DatosAsistenteDTO asistenteDTO;

    @BeforeEach
    void setUp() {
        // Configura datos de prueba básicos antes de cada test
        clienteMock = new Cliente();
        clienteMock.setIdPersona(1);
        // clienteMock.setFechaNacimiento(...) // Necesario para calcularEdad si lo usas

        eventoMock = new Evento();
        eventoMock.setIdEvento(1);
        eventoMock.setNombre("Evento Test");
        eventoMock.setEdadMinima(18);

        tipoTicketMock = new TipoTicket();
        tipoTicketMock.setIdTipoTicket(1);
        tipoTicketMock.setNombre("VIP");
        tipoTicketMock.setPrecio(100.0);
        tipoTicketMock.setCantidadDisponible(10); // Stock inicial
        tipoTicketMock.setEvento(eventoMock);

        asistenteDTO = new DatosAsistenteDTO();
        asistenteDTO.setTipoDocumento(TipoDocumento.DNI);
        asistenteDTO.setNumeroDocumento("12345678");
        asistenteDTO.setNombres("Asis");
        asistenteDTO.setApellidos("Tente");

        itemSeleccionadoDTO = new ItemSeleccionadoDTO();
        itemSeleccionadoDTO.setIdTipoTicket(1);
        itemSeleccionadoDTO.setCantidad(2);
        itemSeleccionadoDTO.setAsistentes(List.of(asistenteDTO, asistenteDTO)); // 2 asistentes

        crearOrdenDTO = new CrearOrdenDTO();
        crearOrdenDTO.setIdCliente(1);
        crearOrdenDTO.setItems(List.of(itemSeleccionadoDTO));
    }

    // --- Tests para crearOrden ---

    @Test
    void testCrearOrden_Exitoso() {
        // Arrange: Configura el comportamiento de los mocks
        // 1. Simula que el cliente existe
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        // 2. Simula que el tipo de ticket existe
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));
        // 3. Simula que hay tickets disponibles
        List<Ticket> ticketsDisponibles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Ticket t = new Ticket(); t.setIdTicket(i+1); t.setEstado(EstadoTicket.DISPONIBLE);
            ticketsDisponibles.add(t);
        }
        when(ticketRepository.findAvailableTicketsByTypeAndState(
                eq(tipoTicketMock), eq(EstadoTicket.DISPONIBLE), any(PageRequest.class))
        ).thenReturn(ticketsDisponibles);
        // 4. Simula la respuesta del save (devuelve el mismo objeto)
        when(ordenCompraRepositorio.save(any(OrdenCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Llama al método del servicio
        OrdenCompra ordenCreada = ordenServicio.crearOrden(crearOrdenDTO);

        // Assert: Verifica los resultados
        assertThat(ordenCreada).isNotNull();
        assertThat(ordenCreada.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(ordenCreada.getCliente()).isEqualTo(clienteMock);
        assertThat(ordenCreada.getFechaExpiracion()).isAfter(LocalDateTime.now().plusMinutes(14)); // Aprox 15 mins
        assertThat(ordenCreada.getItems()).hasSize(1);

        ItemCarrito itemCreado = ordenCreada.getItems().get(0);
        assertThat(itemCreado.getCantidad()).isEqualTo(2);
        assertThat(itemCreado.getPrecio()).isEqualTo(100.0);
        assertThat(itemCreado.getTipoTicket()).isEqualTo(tipoTicketMock);
        assertThat(itemCreado.getTickets()).hasSize(2);

        // Verifica que los tickets se reservaron y se les asignó el asistente
        Ticket ticketReservado = itemCreado.getTickets().get(0);
        assertThat(ticketReservado.getEstado()).isEqualTo(EstadoTicket.RESERVADA);
        assertThat(ticketReservado.getNombreAsistente()).isEqualTo("Asis");

        // Verifica que se llamó al save del repositorio de órdenes
        verify(ordenCompraRepositorio, times(1)).save(any(OrdenCompra.class));
    }

    @Test
    void testCrearOrden_ClienteNoEncontrado() {
        // Arrange: Simula que el cliente NO existe
        when(clienteRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert: Verifica que se lanza la excepción correcta
        assertThatThrownBy(() -> ordenServicio.crearOrden(crearOrdenDTO))
                .isInstanceOf(ResourceNotFoundException.class) // O RuntimeException
                .hasMessageContaining("Cliente no encontrado");

        // Verifica que NO se intentó guardar nada
        verify(ordenCompraRepositorio, never()).save(any(OrdenCompra.class));
    }

    @Test
    void testCrearOrden_TipoTicketNoEncontrado() {
        // Arrange
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ordenServicio.crearOrden(crearOrdenDTO))
                .isInstanceOf(ResourceNotFoundException.class) // O RuntimeException
                .hasMessageContaining("Tipo de ticket no encontrado");
        verify(ordenCompraRepositorio, never()).save(any(OrdenCompra.class));
    }

    @Test
    void testCrearOrden_SinStockSuficiente() {
        // Arrange
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));
        // Simula que solo hay 1 ticket disponible cuando se piden 2
        List<Ticket> ticketsDisponibles = List.of(new Ticket());
        when(ticketRepository.findAvailableTicketsByTypeAndState(
                eq(tipoTicketMock), eq(EstadoTicket.DISPONIBLE), any(PageRequest.class))
        ).thenReturn(ticketsDisponibles);

        // Act & Assert
        assertThatThrownBy(() -> ordenServicio.crearOrden(crearOrdenDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay suficientes tickets disponibles");
        verify(ordenCompraRepositorio, never()).save(any(OrdenCompra.class));
        // Verifica que el stock NO se modificó
        assertThat(tipoTicketMock.getCantidadDisponible()).isEqualTo(10);
    }

    // --- Tests para confirmarPagoOrden --- (Ejemplo básico)

    @Test
    void testConfirmarPagoOrden_Exitoso() {
        // Arrange
        OrdenCompra ordenPendiente = new OrdenCompra();
        ordenPendiente.setIdOrdenCompra(1);
        ordenPendiente.setEstado(EstadoCompra.PENDIENTE);
        ordenPendiente.setFechaExpiracion(LocalDateTime.now().plusMinutes(10)); // No expirada
        // Añadir items y tickets RESERVADOS
        ItemCarrito item = new ItemCarrito();
        Ticket ticket = new Ticket(); ticket.setEstado(EstadoTicket.RESERVADA);
        item.setTickets(List.of(ticket));
        item.setTipoTicket(tipoTicketMock);
        item.setCantidad(1);
        ordenPendiente.setItems(List.of(item));

        when(ordenCompraRepositorio.findById(1)).thenReturn(Optional.of(ordenPendiente));
        when(ordenCompraRepositorio.save(any(OrdenCompra.class))).thenReturn(ordenPendiente); // Devuelve la orden guardada

        // Act
        ordenServicio.confirmarPagoOrden(1);

        // Assert
        assertThat(ordenPendiente.getEstado()).isEqualTo(EstadoCompra.APROBADO);
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.VENDIDA);
        verify(ordenCompraRepositorio, times(1)).save(ordenPendiente);
    }

    // --- Tests para cancelarOrden --- (Ejemplo básico)

    @Test
    void testCancelarOrden_Exitoso() {
        // Arrange
        OrdenCompra ordenPendiente = new OrdenCompra();
        ordenPendiente.setIdOrdenCompra(1);
        ordenPendiente.setEstado(EstadoCompra.PENDIENTE);
        ItemCarrito item = new ItemCarrito(); item.setCantidad(2); item.setTipoTicket(tipoTicketMock);
        Ticket ticket = new Ticket(); ticket.setEstado(EstadoTicket.RESERVADA);
        item.setTickets(List.of(ticket, ticket)); // 2 tickets
        ordenPendiente.setItems(List.of(item));

        when(ordenCompraRepositorio.findById(1)).thenReturn(Optional.of(ordenPendiente));
        when(ordenCompraRepositorio.save(any(OrdenCompra.class))).thenReturn(ordenPendiente);

        // Act
        ordenServicio.cancelarOrden(1);

        // Assert
        assertThat(ordenPendiente.getEstado()).isEqualTo(EstadoCompra.RECHAZADO); // O RECHAZADO
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.DISPONIBLE);
        // Verifica que se devolvió el stock
        assertThat(tipoTicketMock.getCantidadDisponible()).isEqualTo(12); // 10 + 2
        verify(tipoTicketRepositorio, times(1)).save(tipoTicketMock); // Verifica que se guardó el TipoTicket
        verify(ordenCompraRepositorio, times(1)).save(ordenPendiente);
    }

    // --- Tests para anularCompra --- (Ejemplo básico)
    // Similar a cancelar, pero parte de APROBADO, cambia a ANULADO/ANULADA y ajusta cantidadVendida

    // --- Tests para generarResumenOrden --- (Ejemplo básico)

    @Test
    void testGenerarResumenOrden() {
        // Arrange
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));

        // Act
        OrdenResumenDTO resumen = ordenServicio.generarResumenOrden(crearOrdenDTO);

        // Assert
        assertThat(resumen).isNotNull();
        assertThat(resumen.getItems()).hasSize(1);
        assertThat(resumen.getItems().get(0).getNombreTipoTicket()).isEqualTo("VIP");
        assertThat(resumen.getItems().get(0).getCantidad()).isEqualTo(2);
        assertThat(resumen.getSubtotal()).isEqualTo(200.0); // 2 * 100.0
        assertThat(resumen.getTotal()).isEqualTo(200.0);
    }
}