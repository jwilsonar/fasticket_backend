package pe.edu.pucp.fasticket.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.model.eventos.EstadoTicket;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.model.usuario.TipoNivel;
import pe.edu.pucp.fasticket.repository.compra.CarroComprasRepository;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepositorio;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.CarroComprasService;

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
    @Autowired private TicketRepository ticketRepository;
    @Autowired private CarroComprasRepository carroComprasRepository;
    @Autowired private ZonaRepositorio zonaRepository;
    @Autowired private LocalesRepositorio localRepositorio;

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

        // 2. Crear Locales de prueba
        Local local1 = new Local();
        local1.setNombre("Estadio Nacional");
        local1.setDireccion("Av. José Díaz, Lima");
        local1.setAforoTotal(5000);
        local1.setActivo(true);
        local1 = localRepositorio.save(local1);
        
        Local local2 = new Local();
        local2.setNombre("Coliseo Amauta");
        local2.setDireccion("Av. Javier Prado, Lima");
        local2.setAforoTotal(3000);
        local2.setActivo(true);
        local2 = localRepositorio.save(local2);

        // 3. Crear Eventos de prueba (CON TODOS LOS CAMPOS OBLIGATORIOS)
        evento1 = new Evento();
        evento1.setNombre("Concierto de Prueba Finalísimo");
        evento1.setDescripcion("Descripción del concierto de prueba");
        evento1.setFechaEvento(LocalDate.now().plusMonths(3));
        evento1.setHoraInicio(LocalTime.of(20, 0));
        evento1.setTipoEvento(pe.edu.pucp.fasticket.model.eventos.TipoEvento.ROCK);
        evento1.setEstadoEvento(pe.edu.pucp.fasticket.model.eventos.EstadoEvento.ACTIVO);
        evento1.setAforoDisponible(5000);
        evento1.setActivo(true);
        evento1.setFechaCreacion(LocalDate.now());
        evento1.setLocal(local1);
        evento1 = eventosRepositorio.save(evento1);

        Evento evento2 = new Evento();
        evento2.setNombre("Otro Concierto Finalísimo");
        evento2.setDescripcion("Descripción del segundo concierto");
        evento2.setFechaEvento(LocalDate.now().plusMonths(4));
        evento2.setHoraInicio(LocalTime.of(19, 30));
        evento2.setTipoEvento(pe.edu.pucp.fasticket.model.eventos.TipoEvento.POP);
        evento2.setEstadoEvento(pe.edu.pucp.fasticket.model.eventos.EstadoEvento.ACTIVO);
        evento2.setAforoDisponible(3000);
        evento2.setActivo(true);
        evento2.setFechaCreacion(LocalDate.now());
        evento2.setLocal(local2);
        evento2 = eventosRepositorio.save(evento2);

        // 3. Crear Tipos de Ticket
        ticketEvento1 = new TipoTicket();
        ticketEvento1.setNombre("VIP Finalísimo");
        ticketEvento1.setPrecio(250.0);
        ticketEvento1.setCantidadDisponible(5);
        ticketEvento1.setStock(5);
        ticketEvento1.setActivo(true);
        // Crear zonas para los eventos
        Zona zona1 = new Zona();
        zona1.setNombre("Zona VIP Evento 1");
        zona1.setAforoMax(100);
        zona1.setActivo(true);
        zona1.setLocal(local1);
        zona1 = zonaRepository.save(zona1);
        
        Zona zona2 = new Zona();
        zona2.setNombre("Zona General Evento 2");
        zona2.setAforoMax(200);
        zona2.setActivo(true);
        zona2.setLocal(local2);
        zona2 = zonaRepository.save(zona2);
        
        ticketEvento1.setZona(zona1);
        ticketEvento1 = tipoTicketRepository.save(ticketEvento1);

        ticketEvento2 = new TipoTicket();
        ticketEvento2.setNombre("General Finalísimo");
        ticketEvento2.setPrecio(120.0);
        ticketEvento2.setCantidadDisponible(100);
        ticketEvento2.setStock(100);
        ticketEvento2.setActivo(true);
        ticketEvento2.setZona(zona2);
        ticketEvento2 = tipoTicketRepository.save(ticketEvento2);
        
        // 4. Crear tickets individuales para los tipos de ticket
        crearTicketsParaTipoTicket(ticketEvento1, 5); // Solo 5 tickets disponibles
        crearTicketsParaTipoTicket(ticketEvento2, 100);
        
        // 5. Los tipos de ticket ya están relacionados con los eventos a través de las zonas
        // No es necesario agregar manualmente la relación
    }
    
    private void crearTicketsParaTipoTicket(TipoTicket tipoTicket, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            Ticket ticket = new Ticket();
            ticket.setTipoTicket(tipoTicket);
            // Buscar el evento que usa este local
            Evento evento = eventosRepositorio.findByLocalIdLocal(tipoTicket.getZona().getLocal().getIdLocal()).stream()
                    .findFirst()
                    .orElse(null);
            ticket.setEvento(evento);
            ticket.setEstado(EstadoTicket.DISPONIBLE);
            ticket.setPrecio(tipoTicket.getPrecio());
            ticket.setCodigoQr("QR-" + tipoTicket.getIdTipoTicket() + "-" + i);
            ticketRepository.save(ticket);
        }
    }

    @Test
    void testAgregarItemAlCarrito_Exitoso() {
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(2);
        
        // Crear asistentes para la validación
        DatosAsistenteDTO asistente1 = new DatosAsistenteDTO();
        asistente1.setNombres("Asistente 1");
        asistente1.setApellidos("Apellido 1");
        asistente1.setTipoDocumento(TipoDocumento.DNI);
        asistente1.setNumeroDocumento("12345678");
        
        DatosAsistenteDTO asistente2 = new DatosAsistenteDTO();
        asistente2.setNombres("Asistente 2");
        asistente2.setApellidos("Apellido 2");
        asistente2.setTipoDocumento(TipoDocumento.DNI);
        asistente2.setNumeroDocumento("87654321");
        
        request.setAsistentes(List.of(asistente1, asistente2));

        CarroComprasDTO carritoDTO = carroComprasService.agregarItemAlCarrito(request);

        assertNotNull(carritoDTO, "El carritoDTO no debe ser null");
        assertNotNull(carritoDTO.getIdCarro(), "El ID del carrito no debe ser null");
        assertNotNull(carritoDTO.getItems(), "La lista de items no debe ser null");
        assertEquals(1, carritoDTO.getItems().size());
        assertEquals(500.0, carritoDTO.getTotal());
        assertEquals(evento1.getIdEvento(), carroComprasRepository.findById(carritoDTO.getIdCarro()).get().getIdEventoActual());
    }

    @Test
    void testAgregarItem_FallaPorStockInsuficiente() {
        AddItemRequestDTO request = new AddItemRequestDTO();
        request.setIdCliente(clientePrueba.getIdPersona());
        request.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        request.setCantidad(8); // Dentro del límite máximo (10) pero excede el stock (5)
        
        // Crear asistentes para la validación
        List<DatosAsistenteDTO> asistentes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            DatosAsistenteDTO asistente = new DatosAsistenteDTO();
            asistente.setNombres("Asistente " + i);
            asistente.setApellidos("Apellido " + i);
            asistente.setTipoDocumento(TipoDocumento.DNI);
            asistente.setNumeroDocumento("1234567" + i);
            asistentes.add(asistente);
        }
        request.setAsistentes(asistentes);

        Exception exception = assertThrows(Exception.class, () -> carroComprasService.agregarItemAlCarrito(request));
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void testAgregarItem_FallaPorSerDeEventoDiferente() {
        // Crear asistentes para el primer request
        DatosAsistenteDTO asistente1 = new DatosAsistenteDTO();
        asistente1.setNombres("Asistente 1");
        asistente1.setApellidos("Apellido 1");
        asistente1.setTipoDocumento(TipoDocumento.DNI);
        asistente1.setNumeroDocumento("12345678");
        
        AddItemRequestDTO primerRequest = new AddItemRequestDTO();
        primerRequest.setIdCliente(clientePrueba.getIdPersona());
        primerRequest.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        primerRequest.setCantidad(1);
        primerRequest.setAsistentes(List.of(asistente1));
        carroComprasService.agregarItemAlCarrito(primerRequest);

        // Crear asistentes para el segundo request
        DatosAsistenteDTO asistente2 = new DatosAsistenteDTO();
        asistente2.setNombres("Asistente 2");
        asistente2.setApellidos("Apellido 2");
        asistente2.setTipoDocumento(TipoDocumento.DNI);
        asistente2.setNumeroDocumento("87654321");

        AddItemRequestDTO segundoRequest = new AddItemRequestDTO();
        segundoRequest.setIdCliente(clientePrueba.getIdPersona());
        segundoRequest.setIdTipoTicket(ticketEvento2.getIdTipoTicket());
        segundoRequest.setCantidad(1);
        segundoRequest.setAsistentes(List.of(asistente2));

        Exception exception = assertThrows(Exception.class, () -> carroComprasService.agregarItemAlCarrito(segundoRequest));
        assertTrue(exception.getMessage().contains("diferentes eventos"));
    }

    @Test
    void testAgregarItem_FallaPorLimitePorPersona() {
        // Configurar límite por persona en el tipo de ticket
        ticketEvento1.setLimitePorPersona(2);
        tipoTicketRepository.save(ticketEvento1);

        // Crear asistentes para el primer request (2 tickets)
        List<DatosAsistenteDTO> asistentes1 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DatosAsistenteDTO asistente = new DatosAsistenteDTO();
            asistente.setNombres("Asistente " + i);
            asistente.setApellidos("Apellido " + i);
            asistente.setTipoDocumento(TipoDocumento.DNI);
            asistente.setNumeroDocumento("1234567" + i);
            asistentes1.add(asistente);
        }
        
        AddItemRequestDTO primerRequest = new AddItemRequestDTO();
        primerRequest.setIdCliente(clientePrueba.getIdPersona());
        primerRequest.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        primerRequest.setCantidad(2);
        primerRequest.setAsistentes(asistentes1);
        carroComprasService.agregarItemAlCarrito(primerRequest);

        // Intentar agregar más tickets del mismo tipo (debería fallar)
        List<DatosAsistenteDTO> asistentes2 = new ArrayList<>();
        for (int i = 2; i < 4; i++) {
            DatosAsistenteDTO asistente = new DatosAsistenteDTO();
            asistente.setNombres("Asistente " + i);
            asistente.setApellidos("Apellido " + i);
            asistente.setTipoDocumento(TipoDocumento.DNI);
            asistente.setNumeroDocumento("1234567" + i);
            asistentes2.add(asistente);
        }

        AddItemRequestDTO segundoRequest = new AddItemRequestDTO();
        segundoRequest.setIdCliente(clientePrueba.getIdPersona());
        segundoRequest.setIdTipoTicket(ticketEvento1.getIdTipoTicket());
        segundoRequest.setCantidad(2);
        segundoRequest.setAsistentes(asistentes2);

        Exception exception = assertThrows(Exception.class, () -> carroComprasService.agregarItemAlCarrito(segundoRequest));
        assertTrue(exception.getMessage().contains("límite de tickets por persona"));
    }
}