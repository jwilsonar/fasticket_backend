package pe.edu.pucp.fasticket.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemSeleccionadoDTO;
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
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CarroComprasRepository carroComprasRepositorio;
    @Autowired
    private OrdenCompraRepositorio ordenCompraRepositorio;
    @Autowired
    private EventosRepositorio eventoRepositorio;
    @Autowired
    private LocalesRepositorio localRepositorio;
    @Autowired
    private TipoTicketRepositorio tipoTicketRepositorio;
    
    @Autowired
    private ZonaRepositorio zonaRepositorio;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente clienteTest;
    private Evento eventoTest;
    private Local localTest;
    private TipoTicket tipoTicketVip;
    private TipoTicket tipoTicketGeneral;

    @BeforeEach
    void setUp() {
        clienteTest = new Cliente();
        clienteTest.setNombres("Usuario");
        clienteTest.setApellidos("De Prueba");
        clienteTest.setEmail("test" + System.currentTimeMillis() + "@example.com");
        clienteTest.setTipoDocumento(TipoDocumento.DNI);
        clienteTest.setDocIdentidad("12345678");
        clienteTest.setContrasena("password");
        clienteTest.setActivo(true);
        clienteTest = clienteRepository.save(clienteTest);

        localTest = new Local();
        localTest.setNombre("Estadio de Pruebas");
        localTest.setDireccion("Av. Ficticia 123");
        localTest.setAforoTotal(10000);
        localTest.setActivo(true);
        localTest = localRepositorio.save(localTest);

        Evento evento = new Evento();
        evento.setNombre("Concierto de Prueba");
        evento.setDescripcion("Descripción detallada.");
        evento.setFechaEvento(LocalDate.now().plusMonths(2));
        evento.setHoraInicio(LocalTime.of(21, 0));
        // evento.setTipoEvento(TipoEvento.ROCK); // Adjust based on your Enum/String
        // evento.setEstadoEvento(EstadoEvento.ACTIVO); // Adjust based on your Enum/String
        evento.setImagenUrl("http://ejemplo.com/imagen.jpg");
        evento.setActivo(true);
        evento.setLocal(localTest);
        eventoTest = eventoRepositorio.save(evento);

        // Crear zona de prueba
        Zona zonaVip = new Zona();
        zonaVip.setNombre("Zona VIP");
        zonaVip.setAforoMax(500);
        zonaVip.setActivo(true);
        zonaVip.setLocal(localTest);
        zonaVip = zonaRepositorio.save(zonaVip);

        Zona zonaGeneral = new Zona();
        zonaGeneral.setNombre("Zona General");
        zonaGeneral.setAforoMax(2000);
        zonaGeneral.setActivo(true);
        zonaGeneral.setLocal(localTest);
        zonaGeneral = zonaRepositorio.save(zonaGeneral);

        tipoTicketVip = new TipoTicket();
        tipoTicketVip.setNombre("Entrada VIP");
        tipoTicketVip.setPrecio(250.0);
        tipoTicketVip.setStock(500);
        tipoTicketVip.setCantidadDisponible(5);
        tipoTicketVip.setZona(zonaVip);
        tipoTicketVip.setActivo(true);
        tipoTicketVip = tipoTicketRepositorio.save(tipoTicketVip);

        tipoTicketGeneral = new TipoTicket();
        tipoTicketGeneral.setNombre("Entrada General");
        tipoTicketGeneral.setPrecio(100.0);
        tipoTicketGeneral.setStock(2000);
        tipoTicketGeneral.setCantidadDisponible(10);
        tipoTicketGeneral.setZona(zonaGeneral);
        tipoTicketGeneral.setActivo(true);
        tipoTicketGeneral = tipoTicketRepositorio.save(tipoTicketGeneral);

        for (int i = 0; i < 5; i++) {
            Ticket ticket = new Ticket();
            ticket.setTipoTicket(tipoTicketVip);
            ticket.setEvento(eventoTest);
            ticket.setEstado(EstadoTicket.DISPONIBLE);
            ticket.setPrecio(tipoTicketVip.getPrecio());
            ticket.setActivo(true);
            ticketRepository.save(ticket);
        }
        for (int i = 0; i < 10; i++) {
            Ticket ticket = new Ticket();
            ticket.setTipoTicket(tipoTicketGeneral);
            ticket.setEvento(eventoTest);
            ticket.setEstado(EstadoTicket.DISPONIBLE);
            ticket.setPrecio(tipoTicketGeneral.getPrecio());
            ticket.setActivo(true);
            ticketRepository.save(ticket);
        }
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearOrden_ClienteExitoso() throws Exception {
        DatosAsistenteDTO asistente1 = new DatosAsistenteDTO();
        asistente1.setTipoDocumento(TipoDocumento.DNI);
        asistente1.setNumeroDocumento("11111111");
        asistente1.setNombres("Asistente");
        asistente1.setApellidos("Uno");

        DatosAsistenteDTO asistente2 = new DatosAsistenteDTO();
        asistente2.setTipoDocumento(TipoDocumento.DNI);
        asistente2.setNumeroDocumento("22222222");
        asistente2.setNombres("Asistente");
        asistente2.setApellidos("Dos");

        ItemSeleccionadoDTO itemVip = new ItemSeleccionadoDTO();
        itemVip.setIdTipoTicket(tipoTicketVip.getIdTipoTicket());
        itemVip.setCantidad(2);
        itemVip.setAsistentes(List.of(asistente1, asistente2));

        CrearOrdenDTO requestDTO = new CrearOrdenDTO();
        requestDTO.setIdCliente(clienteTest.getIdPersona());
        requestDTO.setItems(List.of(itemVip));

        mockMvc.perform(post("/api/v1/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Proceso iniciado correctamente."))
                .andExpect(jsonPath("$.data.idOrden").exists())
                .andExpect(jsonPath("$.data.total").value(500.0));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearOrden_ClienteSinStock() throws Exception {
        DatosAsistenteDTO asistente = new DatosAsistenteDTO();
        DatosAsistenteDTO asistenteValido = new DatosAsistenteDTO();
        asistenteValido.setTipoDocumento(TipoDocumento.DNI); // O String "DNI"
        asistenteValido.setNumeroDocumento("99999999");
        asistenteValido.setNombres("Asistente");
        asistenteValido.setApellidos("Temporal");
        ItemSeleccionadoDTO itemVip = new ItemSeleccionadoDTO();
        itemVip.setIdTipoTicket(tipoTicketVip.getIdTipoTicket());
        itemVip.setCantidad(10);
        itemVip.setAsistentes(Collections.nCopies(10, asistenteValido));

        CrearOrdenDTO requestDTO = new CrearOrdenDTO();
        requestDTO.setIdCliente(clienteTest.getIdPersona());
        requestDTO.setItems(List.of(itemVip));

        mockMvc.perform(post("/api/v1/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value(containsString("No hay suficientes tickets disponibles")));
    }

    @Test
    void testCrearOrden_NoAutenticado() throws Exception {
        DatosAsistenteDTO asistente = new DatosAsistenteDTO();
        ItemSeleccionadoDTO itemVip = new ItemSeleccionadoDTO();
        itemVip.setIdTipoTicket(tipoTicketVip.getIdTipoTicket());
        itemVip.setCantidad(1);
        itemVip.setAsistentes(List.of(asistente));

        CrearOrdenDTO requestDTO = new CrearOrdenDTO();
        requestDTO.setIdCliente(clienteTest.getIdPersona());
        requestDTO.setItems(List.of(itemVip));

        mockMvc.perform(post("/api/v1/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearOrden_AdminSinPermiso() throws Exception {
        DatosAsistenteDTO asistente = new DatosAsistenteDTO();
        ItemSeleccionadoDTO itemVip = new ItemSeleccionadoDTO();
        itemVip.setIdTipoTicket(tipoTicketVip.getIdTipoTicket());
        itemVip.setCantidad(1);
        itemVip.setAsistentes(List.of(asistente));

        CrearOrdenDTO requestDTO = new CrearOrdenDTO();
        requestDTO.setIdCliente(clienteTest.getIdPersona());
        requestDTO.setItems(List.of(itemVip));

        mockMvc.perform(post("/api/v1/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testObtenerDetalleOrden_ClienteExitoso() throws Exception {
        CarroCompras carro = new CarroCompras();
        carro.setCliente(clienteTest);
        carro = carroComprasRepositorio.save(carro);
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(clienteTest);
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaOrden(LocalDate.now());
        orden.setCarroCompras(carro);

        ItemCarrito item = new ItemCarrito();
        item.setCantidad(1);
        item.setPrecio(tipoTicketVip.getPrecio());
        item.setTipoTicket(tipoTicketVip);
        item.calcularPrecioFinal();

        orden.addItem(item);
        orden.calcularTotal();
        orden = ordenCompraRepositorio.save(orden);

        mockMvc.perform(get("/api/v1/ordenes/" + orden.getIdOrdenCompra()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Proceso iniciado correctamente."))
                .andExpect(jsonPath("$.data.idOrden").value(orden.getIdOrdenCompra()))
                .andExpect(jsonPath("$.data.items[0].nombreTipoTicket").value("Entrada VIP"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testObtenerDetalleOrden_AdminExitoso() throws Exception {
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(clienteTest);
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaOrden(LocalDate.now());
        orden.calcularTotal();
        orden = ordenCompraRepositorio.save(orden);

        mockMvc.perform(get("/api/v1/ordenes/" + orden.getIdOrdenCompra()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testObtenerDetalleOrden_NoEncontrado() throws Exception {
        mockMvc.perform(get("/api/v1/ordenes/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value(containsString("Orden no encontrada")));
    }
}
