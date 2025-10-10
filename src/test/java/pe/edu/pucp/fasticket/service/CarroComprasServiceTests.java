package pe.edu.pucp.fasticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.ClienteRepository;
import pe.edu.pucp.fasticket.repository.TipoTicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
// --- CAMBIO CLAVE ---
// Esta anotación fuerza a Spring a buscar componentes en todos tus paquetes.
@ComponentScan(basePackages = "pe.edu.pucp.fasticket")
public class CarroComprasServiceTests {

    @Autowired private CarroComprasService carroComprasService;
    @Autowired private PersonasRepositorio personasRepositorio;
    @Autowired private EventosRepositorio eventosRepositorio;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TipoTicketRepository tipoTicketRepository;
    @Autowired private CarroComprasRepository carroComprasRepository;

    private Cliente clientePrueba;
    private TipoTicket ticketEvento1;
    private TipoTicket ticketEvento2;
    private Evento evento1;

    @BeforeEach
    void setUp() {
        // 1. Crear una Persona y Cliente de prueba
        Persona persona = new Persona();
        persona.setNombres("UsuarioTestFinal");
        persona.setApellidos("PruebaFinal");
        persona.setTipoDocumento(TipoDocumento.DNI);
        persona.setDocIdentidad("44556677");
        persona.setEmail("test.final.user@pucp.edu.pe");
        persona.setContrasena("clave123");
        persona.setRol(Rol.CLIENTE);
        personasRepositorio.saveAndFlush(persona); // Usamos saveAndFlush para asegurar que esté en la BD

        clientePrueba = new Cliente();
        clientePrueba.setIdPersona(persona.getIdPersona()); // La herencia comparte el ID
        clientePrueba = clienteRepository.saveAndFlush(clientePrueba);

        // 2. Crear Eventos de prueba
        evento1 = new Evento();
        evento1.setNombre("Concierto Definitivo 1");
        evento1 = eventosRepositorio.save(evento1);

        Evento evento2 = new Evento();
        evento2.setNombre("Concierto Definitivo 2");
        evento2 = eventosRepositorio.save(evento2);

        // 3. Crear Tipos de Ticket para cada evento
        ticketEvento1 = new TipoTicket();
        ticketEvento1.setNombre("VIP Final");
        ticketEvento1.setPrecio(200.0);
        ticketEvento1.setCantidadDisponible(30);
        ticketEvento1.setStock(30);
        ticketEvento1.setEvento(evento1);
        ticketEvento1 = tipoTicketRepository.save(ticketEvento1);

        ticketEvento2 = new TipoTicket();
        ticketEvento2.setNombre("General Final");
        ticketEvento2.setPrecio(100.0);
        ticketEvento2.setCantidadDisponible(50);
        ticketEvento2.setStock(50);
        ticketEvento2.setEvento(evento2);
        ticketEvento2 = tipoTicketRepository.save(ticketEvento2);
    }

    // Los métodos de test no cambian, son los mismos de la respuesta anterior.
    @Test
    void testAgregarItemAlCarrito_Exitoso() {
        // Arrange
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(2);

        // Act
        CarroComprasDTO carritoDTO = carroComprasService.agregarItemAlCarrito(request);

        // Assert
        assertNotNull(carritoDTO.getIdCarro(), "El ID del carro no debería ser nulo después de guardar.");
        assertEquals(1, carritoDTO.getItems().size());
        assertEquals(400.0, carritoDTO.getTotal()); // 2 * 200.0
        assertEquals(evento1.getIdEvento(), carroComprasRepository.findById(carritoDTO.getIdCarro()).get().getIdEventoActual());
    }

    @Test
    void testAgregarItem_FallaPorStockInsuficiente() {
        // Arrange
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(35); // Pedimos más del stock disponible (30)

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> carroComprasService.agregarItemAlCarrito(request));
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void testAgregarItem_FallaPorSerDeEventoDiferente() {
        // Arrange: Agregamos un item del evento 1
        AddItemRequestDTO primerRequest = new AddItemRequestDTO();
        primerRequest.setIdCliente(clientePrueba.getIdPersona());
        primerRequest.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        primerRequest.setCantidad(1);
        carroComprasService.agregarItemAlCarrito(primerRequest);

        // Intentamos agregar un item del evento 2
        AddItemRequestDTO segundoRequest = new AddItemRequestDTO();
        segundoRequest.setIdCliente(clientePrueba.getIdPersona());
        segundoRequest.setIdTipoTicket(ticketEvento2.getIdTipoTicket());
        segundoRequest.setCantidad(1);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> carroComprasService.agregarItemAlCarrito(segundoRequest));
        assertTrue(exception.getMessage().contains("diferentes eventos"));
    }
}