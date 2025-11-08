package pe.edu.pucp.fasticket.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemSeleccionadoDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.compra.ItemCarritoRepository;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenCompraRepositorio ordenCompraRepositorio;
    @Mock
    private TipoTicketRepositorio tipoTicketRepositorio;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ItemCarritoRepository itemCarritoRepositorio;
    @Mock
    private CarroComprasRepository carroComprasRepository;
    @Mock
    private FidelizacionService fidelizacionService;

    // --- Instancia del Servicio a probar ---
    @InjectMocks // Crea una instancia de OrdenServicio e inyecta los mocks
    private OrdenServicio ordenServicio;

    // --- Datos de prueba reutilizables ---
    private Cliente clienteMock;
    private Evento eventoMock;
    private Local localMock;
    private TipoTicket tipoTicketMock;
    private CrearOrdenDTO crearOrdenDTO;
    private ItemSeleccionadoDTO itemSeleccionadoDTO;
    private DatosAsistenteDTO asistenteDTO;

    @BeforeEach
    void setUp() {
        // Configura datos de prueba básicos antes de cada test
        clienteMock = new Cliente();
        clienteMock.setIdPersona(1);
        clienteMock.setNivel(TipoMembresia.BRONCE);
        // clienteMock.setFechaNacimiento(...) // Necesario para calcularEdad si lo usas

        localMock = new Local();
        localMock.setIdLocal(1);
        localMock.setNombre("Local Test");
        localMock.setActivo(true);
        
        eventoMock = new Evento();
        eventoMock.setIdEvento(1);
        eventoMock.setNombre("Evento Test");
        eventoMock.setEdadMinima(18);
        eventoMock.setLocal(localMock);

        tipoTicketMock = new TipoTicket();
        tipoTicketMock.setIdTipoTicket(1);
        tipoTicketMock.setNombre("VIP");
        tipoTicketMock.setPrecio(100.0);
        tipoTicketMock.setCantidadDisponible(10); // Stock inicial
        // Crear zona mock
        Zona zonaMock = new Zona();
        zonaMock.setIdZona(1);
        zonaMock.setNombre("Zona VIP");
        zonaMock.setAforoMax(100);
        zonaMock.setActivo(true);
        zonaMock.setEvento(eventoMock);
        
        tipoTicketMock.setZona(zonaMock);

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
        // --- Arrange: Configura mocks esenciales ---
        // Cliente existe
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));

        // Tipo de ticket existe
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));

        // Evento asociado al tipo de ticket
        lenient().when(tipoTicketRepositorio.findEventoByTipoTicket(1))
                .thenReturn(Optional.of(eventoMock));

        // Tickets disponibles
        List<Ticket> ticketsDisponibles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Ticket t = new Ticket();
            t.setIdTicket(i + 1);
            t.setEstado(EstadoTicket.DISPONIBLE);
            t.setTipoTicket(tipoTicketMock);
            ticketsDisponibles.add(t);
        }
        when(ticketRepository.findAvailableTicketsByTypeAndState(
                eq(tipoTicketMock), eq(EstadoTicket.DISPONIBLE), any(PageRequest.class))
        ).thenReturn(ticketsDisponibles);

        // Guardar ItemCarrito devuelve el mismo objeto
        when(itemCarritoRepositorio.save(any(ItemCarrito.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Guardar OrdenCompra asigna ID y devuelve el objeto
        when(ordenCompraRepositorio.save(any(OrdenCompra.class)))
                .thenAnswer(invocation -> {
                    OrdenCompra orden = invocation.getArgument(0);
                    if (orden.getIdOrdenCompra() == null) {
                        orden.setIdOrdenCompra(1); // Simula asignación de ID al guardar
                    }
                    return orden;
                });

        // Guardar tickets devuelve el mismo ticket


        // Fidelización (opcional)
        lenient().when(fidelizacionService.calcularDescuentoPorMembresia(any(), any()))
                .thenReturn(0.0);

        // --- Act ---
        OrdenCompra ordenCreada = ordenServicio.crearOrden(crearOrdenDTO);

        // --- Assert ---
        assertThat(ordenCreada).isNotNull();
        assertThat(ordenCreada.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(ordenCreada.getCliente()).isEqualTo(clienteMock);
        assertThat(ordenCreada.getFechaExpiracion()).isAfter(LocalDateTime.now().plusMinutes(14));
        assertThat(ordenCreada.getItems()).hasSize(1);

        ItemCarrito itemCreado = ordenCreada.getItems().get(0);
        assertThat(itemCreado.getCantidad()).isEqualTo(2);
        assertThat(itemCreado.getPrecio()).isEqualTo(100.0);
        assertThat(itemCreado.getTipoTicket()).isEqualTo(tipoTicketMock);
        assertThat(itemCreado.getTickets()).hasSize(2);

        // Verifica tickets reservados y asignación de asistentes
        Ticket ticketReservado = itemCreado.getTickets().get(0);
        assertThat(ticketReservado.getEstado()).isEqualTo(EstadoTicket.RESERVADA);
        assertThat(ticketReservado.getNombreAsistente()).isEqualTo("Asis");

        // Verifica llamadas a repositorios
        verify(ordenCompraRepositorio, times(2)).save(any(OrdenCompra.class)); // 1 inicial + 1 final
        verify(itemCarritoRepositorio, times(1)).save(any(ItemCarrito.class));
        verify(ticketRepository, times(1)).saveAll(anyList());
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

        CarroCompras carroMock = new CarroCompras();
        carroMock.setIdCarro(99); // ID de prueba
        carroMock.setCliente(clienteMock);

        OrdenCompra ordenPendiente = new OrdenCompra();
        ordenPendiente.setIdOrdenCompra(1);
        ordenPendiente.setEstado(EstadoCompra.PENDIENTE);
        ordenPendiente.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));

        ItemCarrito item = new ItemCarrito();
        Ticket ticket = new Ticket(); ticket.setEstado(EstadoTicket.RESERVADA);
        item.setTickets(List.of(ticket));
        item.setTipoTicket(tipoTicketMock);
        item.setCantidad(1);

        ordenPendiente.setItems(List.of(item));
        ordenPendiente.setCliente(clienteMock);
        ordenPendiente.setCarroCompras(carroMock);
        when(ordenCompraRepositorio.findById(1)).thenReturn(Optional.of(ordenPendiente));
        when(tipoTicketRepositorio.findEventoByTipoTicket(1)).thenReturn(Optional.of(eventoMock));
        when(ordenCompraRepositorio.save(any(OrdenCompra.class))).thenReturn(ordenPendiente);
        doNothing().when(fidelizacionService).generarPuntosPorCompra(any(), any(), any());
        when(carroComprasRepository.save(any(CarroCompras.class))).thenReturn(carroMock);

        ordenServicio.confirmarPagoOrden(1);

        assertThat(ordenPendiente.getEstado()).isEqualTo(EstadoCompra.APROBADO);
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.VENDIDA);
        verify(ordenCompraRepositorio, times(1)).save(ordenPendiente);
        verify(carroComprasRepository, times(2)).save(any(CarroCompras.class));
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

    @Test
    void testCrearOrden_ExcedeLimitePorPersona() {
        // Configurar tipo de ticket con límite por persona
        tipoTicketMock.setLimitePorPersona(2);
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));

        // Simular que el cliente ya compró 2 tickets de este tipo (límite alcanzado)
        when(ticketRepository.countTicketsByClienteAndTipoTicket(1, 1)).thenReturn(2);

        // Crear DTO de orden que excede el límite
        CrearOrdenDTO ordenDTO = new CrearOrdenDTO();
        ordenDTO.setIdCliente(1);

        ItemSeleccionadoDTO item = new ItemSeleccionadoDTO();
        item.setIdTipoTicket(1);
        item.setCantidad(1); // Intentar comprar 1 más cuando ya tiene 2 (límite es 2)

        DatosAsistenteDTO asistente = new DatosAsistenteDTO();
        asistente.setNombres("Test");
        asistente.setApellidos("User");
        asistente.setTipoDocumento(TipoDocumento.DNI);
        asistente.setNumeroDocumento("12345678");
        item.setAsistentes(Collections.singletonList(asistente));

        ordenDTO.setItems(Collections.singletonList(item));

        // Ejecutar y verificar que lanza BusinessException
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ordenServicio.crearOrden(ordenDTO);
        });

        assertTrue(exception.getMessage().contains("límite de tickets por persona"));

        // Verificar que NO se intentó guardar la orden (porque la validación falló antes)
        verify(ordenCompraRepositorio, never()).save(any(OrdenCompra.class));
    }

    @Test
    void testCrearOrden_CreaTicketsCorrectamente() {
        // Configurar TipoTicket con Evento
        eventoMock.setIdEvento(1);
        tipoTicketMock.setEvento(eventoMock);

        // Configurar mocks de Repositorios y Servicios - SOLO LOS ESENCIALES
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        when(tipoTicketRepositorio.findEventoByTipoTicket(1)).thenReturn(Optional.of(eventoMock));
        when(ordenCompraRepositorio.save(any(OrdenCompra.class))).thenAnswer(invocation -> {
            OrdenCompra orden = invocation.getArgument(0);
            orden.setIdOrdenCompra(1);
            return orden;
        });

        when(itemCarritoRepositorio.save(any(ItemCarrito.class)))
                .thenAnswer(invocation -> {
                    ItemCarrito item = invocation.getArgument(0);
                    item.setIdItemCarrito(99);
                    return item;
                });

        // Crear tickets disponibles (con referencias al Evento)
        List<Ticket> ticketsDisponibles = new ArrayList<>();

        Ticket ticket1 = new Ticket();
        ticket1.setIdTicket(1);
        ticket1.setEstado(EstadoTicket.DISPONIBLE);
        ticket1.setTipoTicket(tipoTicketMock);
        ticket1.setEvento(eventoMock);
        ticketsDisponibles.add(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setIdTicket(2);
        ticket2.setEstado(EstadoTicket.DISPONIBLE);
        ticket2.setTipoTicket(tipoTicketMock);
        ticket2.setEvento(eventoMock);
        ticketsDisponibles.add(ticket2);

        when(ticketRepository.findAvailableTicketsByTypeAndState(any(), any(), any()))
                .thenReturn(ticketsDisponibles);

        // Crear DTO de orden
        CrearOrdenDTO ordenDTO = new CrearOrdenDTO();
        ordenDTO.setIdCliente(1);

        ItemSeleccionadoDTO item = new ItemSeleccionadoDTO();
        item.setIdTipoTicket(1);
        item.setCantidad(2);

        DatosAsistenteDTO asistente1 = new DatosAsistenteDTO();
        asistente1.setNombres("Test");
        asistente1.setApellidos("User1");
        asistente1.setTipoDocumento(TipoDocumento.DNI);
        asistente1.setNumeroDocumento("12345678");

        DatosAsistenteDTO asistente2 = new DatosAsistenteDTO();
        asistente2.setNombres("Test");
        asistente2.setApellidos("User2");
        asistente2.setTipoDocumento(TipoDocumento.DNI);
        asistente2.setNumeroDocumento("87654321");

        item.setAsistentes(List.of(asistente1, asistente2));
        ordenDTO.setItems(Collections.singletonList(item));

        // Ejecutar
        OrdenCompra ordenCreada = ordenServicio.crearOrden(ordenDTO);

        // Verificar
        assertThat(ordenCreada).isNotNull();
        assertThat(ordenCreada.getItems()).hasSize(1);
        assertThat(ordenCreada.getItems().get(0).getTickets()).hasSize(2);

        // Verificar que los tickets tienen el evento asignado
        for (Ticket ticket : ordenCreada.getItems().get(0).getTickets()) {
            assertThat(ticket.getEvento()).isNotNull();
            assertThat(ticket.getOrdenCompra()).isNotNull();
            assertThat(ticket.getCliente()).isNotNull();
            assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.RESERVADA);
            assertThat(ticket.getCodigoQr()).isNotNull();
        }

        // Verificar que se guardaron los tickets
        verify(ticketRepository, times(1)).saveAll(anyList());

    }
}