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
import pe.edu.pucp.fasticket.model.usuario.*;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.CarroComprasService;

import java.time.LocalDate; // Importante para la fecha

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
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
        // 1. Crear Cliente de prueba
        Cliente cliente = new Cliente();
        cliente.setNombres("UsuarioFinal");
        cliente.setApellidos("PruebaFinal");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setDocIdentidad("87654321");
        cliente.setEmail("final.test@pucp.edu.pe");
        cliente.setContrasena("clave123");
        cliente.setRol(Rol.CLIENTE);
        cliente.setNivel(TipoNivel.BRONZE);
        clientePrueba = clienteRepository.save(cliente);

        // 2. Crear Eventos de prueba (CON LOS CAMPOS OBLIGATORIOS)
        evento1 = new Evento();
        evento1.setNombre("Concierto de Prueba Finalísimo");
        evento1.setFechaEvento(LocalDate.now().plusMonths(3)); // <-- CAMBIO CLAVE: Añadimos fecha
        evento1 = eventosRepositorio.save(evento1);

        Evento evento2 = new Evento();
        evento2.setNombre("Otro Concierto Finalísimo");
        evento2.setFechaEvento(LocalDate.now().plusMonths(4)); // <-- CAMBIO CLAVE: Añadimos fecha
        evento2 = eventosRepositorio.save(evento2);

        // 3. Crear Tipos de Ticket
        ticketEvento1 = new TipoTicket();
        ticketEvento1.setNombre("VIP Finalísimo");
        ticketEvento1.setPrecio(250.0);
        ticketEvento1.setCantidadDisponible(50);
        ticketEvento1.setStock(50);
        ticketEvento1.setEvento(evento1);
        ticketEvento1 = tipoTicketRepository.save(ticketEvento1);

        ticketEvento2 = new TipoTicket();
        ticketEvento2.setNombre("General Finalísimo");
        ticketEvento2.setPrecio(120.0);
        ticketEvento2.setCantidadDisponible(100);
        ticketEvento2.setStock(100);
        ticketEvento2.setEvento(evento2);
        ticketEvento2 = tipoTicketRepository.save(ticketEvento2);
    }

    // Los tests se mantienen exactamente iguales
    @Test
    void testAgregarItemAlCarrito_Exitoso() {
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(2);

        CarroComprasDTO carritoDTO = carroComprasService.agregarItemAlCarrito(request);

        assertNotNull(carritoDTO.getIdCarro());
        assertEquals(1, carritoDTO.getItems().size());
        assertEquals(500.0, carritoDTO.getTotal());
        assertEquals(evento1.getIdEvento(), carroComprasRepository.findById(carritoDTO.getIdCarro()).get().getIdEventoActual());
    }

    @Test
    void testAgregarItem_FallaPorStockInsuficiente() {
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(55);

        Exception exception = assertThrows(RuntimeException.class, () -> carroComprasService.agregarItemAlCarrito(request));
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void testAgregarItem_FallaPorSerDeEventoDiferente() {
        AddItemRequestDTO primerRequest = new AddItemRequestDTO();
        primerRequest.setIdCliente(clientePrueba.getIdPersona());
        primerRequest.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        primerRequest.setCantidad(1);
        carroComprasService.agregarItemAlCarrito(primerRequest);

        AddItemRequestDTO segundoRequest = new AddItemRequestDTO();
        segundoRequest.setIdCliente(clientePrueba.getIdPersona());
        segundoRequest.setIdTipoTicket(ticketEvento2.getIdTipoTicket());
        segundoRequest.setCantidad(1);

        Exception exception = assertThrows(RuntimeException.class, () -> carroComprasService.agregarItemAlCarrito(segundoRequest));
        assertTrue(exception.getMessage().contains("diferentes eventos"));
    }
}