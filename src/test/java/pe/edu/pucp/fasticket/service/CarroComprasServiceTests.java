package pe.edu.pucp.fasticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.repository.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CarroComprasServiceTests {

    @Autowired
    private CarroComprasService carroComprasService;

    // Inyectamos los repositorios para preparar los datos de prueba
    @Autowired private PersonaRepository personaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private TipoTicketRepository tipoTicketRepository;
    @Autowired private CarroComprasRepository carroComprasRepository;

    // Variables que usaremos en varios tests
    private Cliente clientePrueba;
    private TipoTicket ticketEvento1;
    private TipoTicket ticketEvento2;

    /**
     * Este método se ejecuta ANTES de cada test.
     * Es perfecto para crear los datos base que necesitamos para las pruebas.
     */
    @BeforeEach
    void setUp() {
        // 1. Crear un cliente de prueba
        Persona p = new Persona();
        p.setNombres("Usuario");
        p.setApellidos("De Prueba");
        p.setDocIdentidad("12345678");
        p.setEmail("test@pucp.pe");
        p.setContrasena("123");
        p.setRol(pe.edu.pucp.fasticket.model.usuario.Rol.CLIENTE);
        p = personaRepository.save(p);

        clientePrueba = new Cliente();
        clientePrueba.setPersona(p);
        clientePrueba = clienteRepository.save(clientePrueba);

        // 2. Crear dos eventos de prueba
        Evento evento1 = new Evento();
        evento1.setTitulo("Concierto UB40");
        evento1 = eventoRepository.save(evento1);

        Evento evento2 = new Evento();
        evento2.setTitulo("Concierto Banda 3");
        evento2 = eventoRepository.save(evento2);

        // 3. Crear tipos de tickets para cada evento
        ticketEvento1 = new TipoTicket();
        ticketEvento1.setNombre("VIP");
        ticketEvento1.setPrecio(100.0);
        ticketEvento1.setCantidadDisponible(50);
        ticketEvento1.setEvento(evento1);
        ticketEvento1 = tipoTicketRepository.save(ticketEvento1);

        ticketEvento2 = new TipoTicket();
        ticketEvento2.setNombre("General");
        ticketEvento2.setPrecio(50.0);
        ticketEvento2.setCantidadDisponible(100);
        ticketEvento2.setEvento(evento2);
        ticketEvento2 = tipoTicketRepository.save(ticketEvento2);
    }

    @Test
    void testAgregarItemAlCarrito_Exitoso() {
        // Arrange: Preparar la petición
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(2);

        // Act: Ejecutar el método a probar
        CarroComprasDTO carritoDTO = carroComprasService.agregarItemAlCarrito(request);

        // Assert: Verificar los resultados
        assertNotNull(carritoDTO);
        assertEquals(1, carritoDTO.getItems().size());
        assertEquals(200.0, carritoDTO.getTotal());
        assertEquals("VIP", carritoDTO.getItems().get(0).getNombreTicket());
    }

    @Test
    void testAgregarItem_FallaPorStockInsuficiente() {
        // Arrange
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(100); // Pedimos más del stock disponible (50)

        // Act & Assert
        // Verificamos que al ejecutar el método, se lance una excepción del tipo RuntimeException
        Exception exception = assertThrows(RuntimeException.class, () -> {
            carroComprasService.agregarItemAlCarrito(request);
        });

        // Opcional: Verificamos que el mensaje de error sea el que esperamos
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void testAgregarItem_FallaPorSerDeEventoDiferente() {
        // Arrange: Primero agregamos un item del evento 1
        AddItemRequestDTO primerRequest = new AddItemRequestDTO();
        primerRequest.setIdCliente(clientePrueba.getIdPersona());
        primerRequest.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        primerRequest.setCantidad(1);
        carroComprasService.agregarItemAlCarrito(primerRequest);

        // Ahora intentamos agregar un item del evento 2
        AddItemRequestDTO segundoRequest = new AddItemRequestDTO();
        segundoRequest.setIdCliente(clientePrueba.getIdPersona());
        segundoRequest.setIdTipoTicket(ticketEvento2.getIdTipoTicket()); // <- Ticket del otro evento
        segundoRequest.setCantidad(1);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            carroComprasService.agregarItemAlCarrito(segundoRequest);
        });

        assertTrue(exception.getMessage().contains("diferentes eventos"));
    }
}