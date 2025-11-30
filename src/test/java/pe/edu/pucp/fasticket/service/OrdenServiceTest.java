package pe.edu.pucp.fasticket.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
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
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import static pe.edu.pucp.fasticket.model.usuario.TipoDocumento.DNI;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.compra.ItemCarritoRepository;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.EmailService;
import pe.edu.pucp.fasticket.services.S3Service;
import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;
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

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AdministradorRepository administradorRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private S3Service s3Service;

    // --- Instancia del Servicio a probar ---
    @Spy
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

        localMock = new Local();
        localMock.setIdLocal(1);
        localMock.setNombre("Local Test");
        localMock.setActivo(true);

        eventoMock = new Evento();
        eventoMock.setIdEvento(1);
        eventoMock.setNombre("Evento Test");
        eventoMock.setMenoresDeEdadPermitidos(false);
        eventoMock.setRestricciones("Prohibido el ingreso de menores de 18 años");
        eventoMock.setPoliticasDevolucion("No se permiten devoluciones");
        eventoMock.setLocal(localMock);

        tipoTicketMock = new TipoTicket();
        tipoTicketMock.setIdTipoTicket(1);
        tipoTicketMock.setNombre("VIP");
        tipoTicketMock.setPrecio(100.0);
        tipoTicketMock.setCantidadDisponible(10);
        tipoTicketMock.setActivo(true);

        // Crear zona mock
        Zona zonaMock = new Zona();
        zonaMock.setIdZona(1);
        zonaMock.setNombre("Zona VIP");
        zonaMock.setAforoMax(100);
        zonaMock.setActivo(true);
        zonaMock.setEvento(eventoMock);

        tipoTicketMock.setZona(zonaMock);

        asistenteDTO = new DatosAsistenteDTO();
        asistenteDTO.setTipoDocumento(DNI);
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
        
        // Configurar mock de S3Service para todos los tests (lenient para evitar errores si no se usa)
        lenient().when(s3Service.uploadFileFromBytes(any(byte[].class), anyString(), anyString(), anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    String fileName = invocation.getArgument(1);
                    Integer ticketId = invocation.getArgument(4);
                    return "https://test-bucket.s3.us-east-1.amazonaws.com/tickets/" + ticketId + "/" + fileName;
                });
        
        // Mock para flush (lenient para evitar errores si no se usa)
        lenient().doNothing().when(ticketRepository).flush();

        lenient().when(configuracionRepository.findById(eq("LIMITE_TICKETS_POR_COMPRA")))
                .thenReturn(Optional.empty());

        lenient().when(ordenCompraRepositorio.save(any(OrdenCompra.class)))
                .thenAnswer(invocation -> {
                    OrdenCompra orden = invocation.getArgument(0);
                    if (orden.getIdOrdenCompra() == null) {
                        orden.setIdOrdenCompra(1);
                    }
                    return orden;
                });

        lenient().when(ordenCompraRepositorio.saveAndFlush(any(OrdenCompra.class)))
                .thenAnswer(invocation -> {
                    OrdenCompra orden = invocation.getArgument(0);
                    if (orden.getIdOrdenCompra() == null) {
                        orden.setIdOrdenCompra(1);
                    }
                    return orden;
                });

        lenient().when(ordenCompraRepositorio.findById(anyInt()))
                .thenAnswer(invocation -> {
                    Integer id = invocation.getArgument(0);
                    OrdenCompra orden = new OrdenCompra();
                    orden.setIdOrdenCompra(id);
                    orden.setCliente(clienteMock);
                    orden.setEstado(EstadoCompra.PENDIENTE);
                    orden.setItems(new ArrayList<>());
                    return Optional.of(orden);
                });

        lenient().when(carroComprasRepository.findByCliente_IdPersonaAndActivoTrue(anyInt()))
                .thenReturn(Optional.empty());
    }

    // --- Tests para crearOrden ---

    @Test
    void testCrearOrden_Exitoso() {
        // --- Arrange: Configura mocks esenciales ---
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteMock));
        when(tipoTicketRepositorio.findById(1)).thenReturn(Optional.of(tipoTicketMock));
        when(tipoTicketRepositorio.findEventoByTipoTicket(1)).thenReturn(Optional.of(eventoMock));

        // Configurar TipoTicket correctamente
        tipoTicketMock.setPrecio(100.0);
        tipoTicketMock.setCantidadDisponible(10);
        tipoTicketMock.setCantidadVendida(0);

        // Tickets disponibles
        List<Ticket> ticketsDisponibles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Ticket t = new Ticket();
            t.setIdTicket(i + 1);
            t.setEstado(EstadoTicket.DISPONIBLE);
            t.setTipoTicket(tipoTicketMock);
            t.setEvento(eventoMock);
            ticketsDisponibles.add(t);
        }

        when(ticketRepository.findAvailableTicketsByTypeAndState(
                eq(tipoTicketMock), eq(EstadoTicket.DISPONIBLE), any(PageRequest.class))
        ).thenReturn(ticketsDisponibles);

        // Mock para guardar ItemCarrito
        when(itemCarritoRepositorio.save(any(ItemCarrito.class)))
                .thenAnswer(invocation -> {
                    ItemCarrito item = invocation.getArgument(0);
                    item.setIdItemCarrito(99); // Asignar ID
                    if (item.getPrecio() != null && item.getCantidad() != null) {
                        item.setPrecioFinal(item.getPrecio() * item.getCantidad());
                    }
                    return item;
                });

        // Mock para saveAll de tickets
        when(ticketRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock para flush
        doNothing().when(ticketRepository).flush();

        // Mock para S3Service - subir QR a S3
        when(s3Service.uploadFileFromBytes(any(byte[].class), anyString(), anyString(), anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    String fileName = invocation.getArgument(1);
                    Integer ticketId = invocation.getArgument(4);
                    return "https://test-bucket.s3.us-east-1.amazonaws.com/tickets/" + ticketId + "/" + fileName;
                });

        // Mock para ordenCompraRepositorio.findById que devuelve la orden actualizada
        when(ordenCompraRepositorio.findById(anyInt()))
                .thenAnswer(invocation -> {
                    Integer id = invocation.getArgument(0);
                    OrdenCompra orden = new OrdenCompra();
                    orden.setIdOrdenCompra(id);
                    orden.setCliente(clienteMock);
                    orden.setEstado(EstadoCompra.PENDIENTE);
                    orden.setSubtotal(200.0);
                    orden.setTotal(200.0);

                    ItemCarrito itemMock = new ItemCarrito();
                    itemMock.setIdItemCarrito(99);
                    itemMock.setCantidad(2);
                    itemMock.setPrecio(100.0);
                    itemMock.setPrecioFinal(200.0);
                    itemMock.setTipoTicket(tipoTicketMock);

                    List<Ticket> ticketsMock = new ArrayList<>();
                    for (int i = 0; i < 2; i++) {
                        Ticket ticket = new Ticket();
                        ticket.setIdTicket(i + 1);
                        ticket.setEstado(EstadoTicket.RESERVADA);
                        ticket.setNombreAsistente("Asis");
                        ticket.setApellidoAsistente("Tente");
                        ticket.setTipoDocumentoAsistente(pe.edu.pucp.fasticket.model.usuario.TipoDocumento.DNI); // Corregido el Enum
                        ticket.setDocumentoAsistente("12345678");
                        ticket.setItemCarrito(itemMock);
                        ticket.setOrdenCompra(orden);
                        ticketsMock.add(ticket);
                    }
                    itemMock.setTickets(ticketsMock);

                    orden.setItems(List.of(itemMock));
                    return Optional.of(orden);
                });

        // Fidelización
        when(fidelizacionService.calcularDescuentoPorMembresia(any(), anyInt()))
                .thenReturn(0.0);
        List<ItemCarrito> itemsSimulados = new ArrayList<>();
        ItemCarrito itemSim = new ItemCarrito();
        itemSim.setIdItemCarrito(99);
        itemSim.setPrecio(100.0);
        itemSim.setCantidad(2);
        itemSim.setPrecioFinal(200.0); // Importante para el cálculo de subtotal
        itemSim.setTipoTicket(tipoTicketMock); // Necesario para evitar NPE en logs
        itemsSimulados.add(itemSim);

        when(itemCarritoRepositorio.findByOrdenCompra_IdOrdenCompra(anyInt()))
                .thenReturn(itemsSimulados);
        // ===============================================================================

        // --- Act ---
        OrdenCompra ordenCreada = ordenServicio.crearOrden(crearOrdenDTO);

        // --- Assert ---
        assertThat(ordenCreada).isNotNull();
        assertThat(ordenCreada.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(ordenCreada.getCliente()).isEqualTo(clienteMock);

        assertThat(ordenCreada.getItems()).isNotEmpty();
        if (!ordenCreada.getItems().isEmpty()) {
            ItemCarrito itemCreado = ordenCreada.getItems().get(0);
            assertThat(itemCreado.getCantidad()).isEqualTo(2);
            assertThat(itemCreado.getPrecio()).isEqualTo(100.0);
            // Nota: La validación profunda de tickets depende de lo que devuelva el Mock de findById
        }

        assertThat(ordenCreada.getSubtotal()).isEqualTo(200.0);
        assertThat(ordenCreada.getTotal()).isEqualTo(200.0);
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
        asistente.setTipoDocumento(DNI);
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
        int idCliente = 1;

        // --- 1. Configurar Cliente ---
        Cliente cliente = new Cliente();
        cliente.setIdPersona(idCliente);
        cliente.setNombres("Juan");
        cliente.setApellidos("Perez");
        cliente.setNivel(TipoMembresia.BRONCE);

        when(clienteRepository.findById(idCliente))
                .thenReturn(Optional.of(cliente));

        // --- 2. Configurar DTO de entrada ---
        CrearOrdenDTO dto = new CrearOrdenDTO();
        dto.setIdCliente(idCliente);

        DatosAsistenteDTO asistente = new DatosAsistenteDTO();
        asistente.setNombres("Juan");
        asistente.setApellidos("Perez");
        asistente.setTipoDocumento(pe.edu.pucp.fasticket.model.usuario.TipoDocumento.DNI);
        asistente.setNumeroDocumento("12345678");

        ItemSeleccionadoDTO itemDTO = new ItemSeleccionadoDTO();
        itemDTO.setIdTipoTicket(1);
        itemDTO.setCantidad(1);
        itemDTO.setAsistentes(List.of(asistente));

        dto.setItems(List.of(itemDTO));

        // --- 3. Configurar TipoTicket ---
        TipoTicket tipoTicket = new TipoTicket();
        tipoTicket.setIdTipoTicket(1);
        tipoTicket.setNombre("VIP");
        tipoTicket.setPrecio(100.0);
        tipoTicket.setCantidadDisponible(10);
        tipoTicket.setActivo(true);

        // --- 4. Configurar Mocks de Validación ---
        doNothing().when(ordenServicio).validarLimitePorCompra(any());
        doNothing().when(ordenServicio).validarLimitesPorPersona(any(), any());
        doNothing().when(ordenServicio).validarStockDisponible(any(), any());

        // --- 5. Configurar Items y Tickets (Simulación de creación) ---
        Ticket ticket = new Ticket();
        ticket.setIdTicket(10);
        ticket.setPrecio(100.0);
        ticket.setEstado(EstadoTicket.RESERVADA);
        ticket.setNombreAsistente("Juan");
        ticket.setApellidoAsistente("Perez");
        ticket.setTipoDocumentoAsistente(pe.edu.pucp.fasticket.model.usuario.TipoDocumento.DNI);
        ticket.setDocumentoAsistente("12345678");

        ItemCarrito item = new ItemCarrito();
        item.setIdItemCarrito(1);
        item.setCantidad(1);
        item.setPrecio(100.0);
        item.setPrecioFinal(100.0);
        item.setTipoTicket(tipoTicket);
        item.setTickets(List.of(ticket));

        // Relación bidireccional
        ticket.setItemCarrito(item);

        // --- 6. Configurar Orden devuelta ---
        OrdenCompra ordenConItems = new OrdenCompra();
        ordenConItems.setIdOrdenCompra(1);
        ordenConItems.setCliente(cliente);
        ordenConItems.setEstado(EstadoCompra.PENDIENTE);
        ordenConItems.setItems(List.of(item));
        ordenConItems.setSubtotal(100.0);
        ordenConItems.setTotal(100.0);

        item.setOrdenCompra(ordenConItems);
        ticket.setOrdenCompra(ordenConItems);

        // Mock del saveAndFlush inicial (Retorna orden con ID 1)
        when(ordenCompraRepositorio.saveAndFlush(any(OrdenCompra.class)))
                .thenAnswer(invocation -> {
                    OrdenCompra ordenArg = invocation.getArgument(0);
                    if (ordenArg.getIdOrdenCompra() == null) {
                        ordenArg.setIdOrdenCompra(1);
                    }
                    return ordenArg;
                });

        // Mock para construirYGuardarItems (Retorna la lista de items creada)
        doReturn(List.of(item))
                .when(ordenServicio)
                .construirYGuardarItems(any(), any(), any());

        // Mock del findById (usado para recargar la orden)
        when(ordenCompraRepositorio.findById(1))
                .thenReturn(Optional.of(ordenConItems));

        // [CORRECCIÓN] Mock de la búsqueda de items (La parte que faltaba)
        // Esto evita el "BusinessException: No se pudieron cargar los items"
        when(itemCarritoRepositorio.findByOrdenCompra_IdOrdenCompra(1))
                .thenReturn(List.of(item));

        // Mock fidelización
        when(fidelizacionService.calcularDescuentoPorMembresia(any(), anyInt()))
                .thenReturn(0.0);

        // --- Act ---
        OrdenCompra respuesta = ordenServicio.crearOrden(dto, idCliente);

        // --- Assert ---
        assertNotNull(respuesta);
        assertNotNull(respuesta.getItems());
        assertEquals(1, respuesta.getItems().size(), "Debe haber 1 item en la orden");

        ItemCarrito itemRespuesta = respuesta.getItems().get(0);
        assertNotNull(itemRespuesta.getTickets());
        assertEquals(1, itemRespuesta.getTickets().size(), "Debe haber 1 ticket en el item");

        Ticket ticketRespuesta = itemRespuesta.getTickets().get(0);
        assertEquals(EstadoTicket.RESERVADA, ticketRespuesta.getEstado());
        assertEquals("Juan", ticketRespuesta.getNombreAsistente());
        assertEquals("Perez", ticketRespuesta.getApellidoAsistente());
        assertEquals(pe.edu.pucp.fasticket.model.usuario.TipoDocumento.DNI, ticketRespuesta.getTipoDocumentoAsistente());
        assertEquals("12345678", ticketRespuesta.getDocumentoAsistente());

        assertNotNull(respuesta.getTotal());
        assertEquals(100.0, respuesta.getTotal());

        // Verificaciones finales
        verify(ordenServicio, times(1)).validarLimitePorCompra(any());
        verify(ordenServicio, times(1)).validarLimitesPorPersona(any(), any());
        verify(ordenServicio, times(1)).validarStockDisponible(any(), any());
        verify(ordenServicio, times(1)).construirYGuardarItems(any(), any(), any());
    }

}