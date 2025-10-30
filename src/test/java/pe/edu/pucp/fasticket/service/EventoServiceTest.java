// Make sure this package name is correct
package pe.edu.pucp.fasticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// --- Necessary Imports ---
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
// Using wildcard import for models for simplicity in test
import pe.edu.pucp.fasticket.model.eventos.*;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;
import pe.edu.pucp.fasticket.services.eventos.EventoService;
// --- End Imports ---

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class EventoServiceTest {

    // --- CORRECT INJECTIONS (ONCE AT THE TOP) ---
    @Autowired
    private EventoService eventoService;
    @Autowired
    private LocalesRepositorio localRepository;
    @Autowired
    private EventosRepositorio eventoRepository;
    @Autowired
    private TicketRepositorio ticketRepository;
    @Autowired
    private TipoTicketRepository tipoTicketRepository;
    @Autowired
    private OrdenCompraRepositorio ordenCompraRepository;
    // --- END INJECTIONS ---

    private Local localTest;

    @BeforeEach
    void setUp() {
        Local local = new Local();
        local.setNombre("Estadio Test Setup");
        local.setDireccion("Av. Setup 123");
        local.setAforoTotal(10000);
        local.setActivo(true);
        // Assuming fechaCreacion is handled by auditing or defaults
        localTest = localRepository.save(local);
    }

    @Test
    void testCrearEvento_Exitoso_GuardaComoBorrador() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Concierto Rock Borrador");
        dto.setDescripcion("Gran concierto de rock");
        dto.setFechaEvento(LocalDate.now().plusMonths(2));
        dto.setHoraInicio(LocalTime.of(20, 0));
        dto.setTipoEvento(TipoEvento.ROCK); // Assuming TipoEvento is an Enum in model
        dto.setAforoDisponible(5000); // Assuming this field exists in DTO
        dto.setIdLocal(localTest.getIdLocal());

        // Act
        EventoResponseDTO response = eventoService.crear(dto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getIdEvento());
        assertEquals("Concierto Rock Borrador", response.getNombre());
        // Check Enum if TipoEvento is Enum in DTO/Response
        assertEquals(TipoEvento.ROCK, response.getTipoEvento());
        assertEquals(EstadoEvento.BORRADOR, response.getEstadoEvento());
        assertFalse(response.getActivo());
    }

    @Test
    void testCrearEvento_FechaPasada() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Pasado");
        dto.setFechaEvento(LocalDate.now().minusDays(1));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setAforoDisponible(1000);
        dto.setIdLocal(localTest.getIdLocal()); // Need a local

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> eventoService.crear(dto));
        assertTrue(exception.getMessage().contains("debe ser futura"));
    }

    @Test
    void testListarEventosActivos_NoDebeIncluirBorradores() {
        // Arrange - Crear un evento BORRADOR
        EventoCreateDTO dtoBorrador = new EventoCreateDTO(); // <-- CONSTRUCTOR VACÍO
        dtoBorrador.setNombre("Borrador Listar");
        dtoBorrador.setFechaEvento(LocalDate.now().plusDays(10));
        dtoBorrador.setTipoEvento(TipoEvento.ROCK);
        dtoBorrador.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO borrador = eventoService.crear(dtoBorrador);

        // Arrange - Crear y PUBLICAR otro evento
        EventoCreateDTO dtoPublicado = new EventoCreateDTO(); // <-- CONSTRUCTOR VACÍO
        dtoPublicado.setNombre("Publicado Listar");
        dtoPublicado.setFechaEvento(LocalDate.now().plusDays(20));
        dtoPublicado.setTipoEvento(TipoEvento.POP);
        dtoPublicado.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO publicadoBorrador = eventoService.crear(dtoPublicado);

        // --- CORRECCIÓN: Simular adición de TipoTicket ANTES de publicar ---

        // 1. Obtener la entidad Evento
        Evento eventoEntityPub = eventoRepository.findById(publicadoBorrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("Setup failed: Evento borrador (para publicar) no encontrado"));

        // 2. Crear y asociar el TipoTicket
        TipoTicket tipoTicketPub = new TipoTicket();
        tipoTicketPub.setNombre("General Pub");
        tipoTicketPub.setPrecio(1.0);
        tipoTicketPub.setStock(100);
        tipoTicketPub.setCantidadDisponible(100);
        tipoTicketPub.setActivo(true);
        tipoTicketPub.setEvento(eventoEntityPub);

        // 3. Añadir a la colección y guardar
        if (eventoEntityPub.getTiposTicket() == null) {
            eventoEntityPub.setTiposTicket(new java.util.ArrayList<>());
        }
        eventoEntityPub.getTiposTicket().add(tipoTicketPub);
        tipoTicketRepository.save(tipoTicketPub);

        // --- FIN CORRECCIÓN ---

        // 4. Publicar el evento (Ahora debería funcionar)
        EventoResponseDTO publicadoFinal = eventoService.publicarEvento(publicadoBorrador.getIdEvento());

        // Act - Listar solo los activos
        List<EventoResponseDTO> eventosActivos = eventoService.listarActivos();

        // Assert
        assertTrue(eventosActivos.stream().anyMatch(e -> e.getIdEvento().equals(publicadoFinal.getIdEvento())),
                "El evento publicado debe estar en la lista de activos");
        assertTrue(eventosActivos.stream().noneMatch(e -> e.getIdEvento().equals(borrador.getIdEvento())),
                "El evento borrador NO debe estar en la lista de activos");
    }

    @Test
    void testListarEventosProximos() {
        // Arrange - Crear evento BORRADOR próximo
        EventoCreateDTO dtoPublicado = new EventoCreateDTO();
        dtoPublicado.setNombre("Evento Próximo Publicado Test");
        dtoPublicado.setFechaEvento(LocalDate.now().plusDays(5)); // Próximo
        dtoPublicado.setTipoEvento(TipoEvento.ELECTRONICA); // Use Enum
        dtoPublicado.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO publicadoBorrador = eventoService.crear(dtoPublicado);

        // --- CORRECCIÓN: Simular adición de TipoTicket ANTES de publicar ---

        // 1. Obtener la entidad Evento recién creada
        Evento eventoEntityProx = eventoRepository.findById(publicadoBorrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("Setup failed: Evento borrador no encontrado"));

        // 2. Crear el TipoTicket de prueba
        TipoTicket tipoTicketProx = new TipoTicket();
        tipoTicketProx.setNombre("General Prox");
        tipoTicketProx.setPrecio(1.0);
        tipoTicketProx.setStock(100);
        tipoTicketProx.setCantidadDisponible(100);
        tipoTicketProx.setActivo(true);
        tipoTicketProx.setEvento(eventoEntityProx); // Asocia el TipoTicket al evento

        // 3. ¡IMPORTANTE! Añadir el TipoTicket a la colección del evento
        if (eventoEntityProx.getTiposTicket() == null) {
            eventoEntityProx.setTiposTicket(new java.util.ArrayList<>());
        }
        eventoEntityProx.getTiposTicket().add(tipoTicketProx);

        // 4. Guardar el TipoTicket
        tipoTicketRepository.save(tipoTicketProx);

        // --- FIN CORRECCIÓN ---

        // 5. Publicar el evento (Ahora debería funcionar)
        EventoResponseDTO publicadoFinal = eventoService.publicarEvento(publicadoBorrador.getIdEvento());

        // Act - Listar eventos próximos
        List<EventoResponseDTO> eventosProximos = eventoService.listarProximos();

        // Assert
        assertFalse(eventosProximos.isEmpty(), "La lista de próximos no debe estar vacía");
        assertTrue(eventosProximos.stream().anyMatch(e -> e.getIdEvento().equals(publicadoFinal.getIdEvento())),
                "El evento próximo publicado debe estar en la lista");
        // Opcional: Verificar que solo vengan eventos futuros y publicados
        assertTrue(eventosProximos.stream().allMatch(e -> e.getFechaEvento().isAfter(LocalDate.now().minusDays(1))),
                "Todos los eventos listados deben ser futuros");
    }

    @Test
    void testActualizarEvento_Exitoso() {
        // Arrange - Crear evento BORRADOR
        EventoCreateDTO dtoCrear = new EventoCreateDTO();
        dtoCrear.setNombre("Evento Original Update");
        dtoCrear.setFechaEvento(LocalDate.now().plusMonths(1));
        dtoCrear.setTipoEvento(TipoEvento.ROCK);
        dtoCrear.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO eventoCreado = eventoService.crear(dtoCrear);

        // Preparar actualización
        EventoCreateDTO dtoActualizar = new EventoCreateDTO();
        dtoActualizar.setNombre("Evento Actualizado Update");
        dtoActualizar.setFechaEvento(LocalDate.now().plusMonths(2));
        dtoActualizar.setTipoEvento(TipoEvento.POP);
        dtoActualizar.setAforoDisponible(1500); // Assuming this field exists
        dtoActualizar.setIdLocal(localTest.getIdLocal());

        // Act
        EventoResponseDTO eventoActualizado = eventoService.actualizar(eventoCreado.getIdEvento(), dtoActualizar);

        // Assert
        assertEquals("Evento Actualizado Update", eventoActualizado.getNombre());
        assertEquals(TipoEvento.POP, eventoActualizado.getTipoEvento());
        assertEquals(1500, eventoActualizado.getAforoDisponible());
        assertEquals(EstadoEvento.BORRADOR, eventoActualizado.getEstadoEvento());
    }

    @Test
    void testObtenerEventoPorId_NoExiste() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> eventoService.obtenerPorId(99999));
        assertTrue(exception.getMessage().contains("Evento no encontrado"));
    }

    @Test
    void testEliminarEvento_Logico() {
        // Arrange - Crear evento BORRADOR
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento a Eliminar");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO evento = eventoService.crear(dto);

        // Act
        eventoService.eliminarLogico(evento.getIdEvento());

        // Assert - El evento debe estar inactivo
        Evento eventoConsultadoDirecto = eventoRepository.findById(evento.getIdEvento()).orElse(null);
        assertNotNull(eventoConsultadoDirecto, "El evento aún debe existir en la BD");
        assertFalse(eventoConsultadoDirecto.getActivo(), "El evento debe estar marcado como inactivo");
    }

    @Test
    void testListarEventosPorEstado_Borrador() {
        // Arrange - Crear evento BORRADOR
        EventoCreateDTO dtoBorrador = new EventoCreateDTO();
        dtoBorrador.setNombre("Evento en Borrador Listar");
        dtoBorrador.setFechaEvento(LocalDate.now().plusDays(15));
        dtoBorrador.setTipoEvento(TipoEvento.ROCK);
        dtoBorrador.setIdLocal(localTest.getIdLocal());
        eventoService.crear(dtoBorrador);

        // Act
        List<EventoResponseDTO> eventosBorrador = eventoService.listarPorEstado(EstadoEvento.BORRADOR);

        // Assert
        assertFalse(eventosBorrador.isEmpty(), "Debe encontrar el evento en estado BORRADOR");
        assertTrue(eventosBorrador.stream()
                .allMatch(e -> e.getEstadoEvento() == EstadoEvento.BORRADOR));
    }

    @Test
    void testPublicarEvento_Exitoso() {
        // Arrange - Crear evento BORRADOR
        EventoCreateDTO dtoCrear = new EventoCreateDTO();
        dtoCrear.setNombre("Evento para Publicar OK");
        dtoCrear.setFechaEvento(LocalDate.now().plusMonths(1));
        dtoCrear.setTipoEvento(TipoEvento.ROCK);
        dtoCrear.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO eventoBorrador = eventoService.crear(dtoCrear);

        // --- CORRECCIÓN: Simular la creación de un TipoTicket (Paso 3 del wizard) ---

        // 1. Obtener la entidad Evento recién creada
        Evento eventoEntity = eventoRepository.findById(eventoBorrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("El evento borrador no se guardó correctamente"));

        // 2. Crear el TipoTicket de prueba
        TipoTicket tipoTicketDePrueba = new TipoTicket();
        tipoTicketDePrueba.setNombre("Entrada General Test");
        tipoTicketDePrueba.setPrecio(100.0);
        tipoTicketDePrueba.setStock(500); // Stock inicial
        tipoTicketDePrueba.setCantidadDisponible(500);
        tipoTicketDePrueba.setActivo(true);
        tipoTicketDePrueba.setEvento(eventoEntity); // Asocia el TipoTicket al evento

        // 3. ¡IMPORTANTE! Añadir el TipoTicket a la colección del evento
        if (eventoEntity.getTiposTicket() == null) {
            eventoEntity.setTiposTicket(new java.util.ArrayList<>());
        }
        eventoEntity.getTiposTicket().add(tipoTicketDePrueba);

        // 4. Guardar el TipoTicket (¡Necesitas el TipoTicketRepositorio!)
        //    (Asegúrate de tener @Autowired private TipoTicketRepositorio tipoTicketRepository; arriba)
        tipoTicketRepository.save(tipoTicketDePrueba);

        // --- FIN CORRECCIÓN ---

        // Act - Intentar publicar AHORA
        EventoResponseDTO eventoPublicado = eventoService.publicarEvento(eventoBorrador.getIdEvento());

        // Assert
        assertNotNull(eventoPublicado);
        assertEquals(EstadoEvento.PUBLICADO, eventoPublicado.getEstadoEvento());
        assertTrue(eventoPublicado.getActivo());
    }

    @Test
    void testPublicarEvento_FallaSiNoEsBorrador() {
        // Arrange - Crear evento BORRADOR
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Ya Publicado Test");
        dto.setFechaEvento(LocalDate.now().plusMonths(1));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO borrador = eventoService.crear(dto);

        // --- CORRECCIÓN: Simular adición de TipoTicket ANTES de la 1ra publicación ---
        // 1. Obtener la entidad Evento
        Evento eventoEntityPub = eventoRepository.findById(borrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("Setup failed: Evento borrador no encontrado"));

        // 2. Crear y asociar el TipoTicket
        TipoTicket tipoTicketPub = new TipoTicket();
        tipoTicketPub.setNombre("Entrada General");
        tipoTicketPub.setPrecio(1.0);
        tipoTicketPub.setStock(100);
        tipoTicketPub.setCantidadDisponible(100);
        tipoTicketPub.setActivo(true);
        tipoTicketPub.setEvento(eventoEntityPub);

        // 3. Añadir a la colección y guardar
        if (eventoEntityPub.getTiposTicket() == null) {
            eventoEntityPub.setTiposTicket(new java.util.ArrayList<>());
        }
        eventoEntityPub.getTiposTicket().add(tipoTicketPub);
        tipoTicketRepository.save(tipoTicketPub);
        // --- FIN CORRECCIÓN ---

        // 4. Publicar el evento por PRIMERA vez (esto debe funcionar ahora)
        eventoService.publicarEvento(borrador.getIdEvento());

        // Act & Assert - Intentar publicar de NUEVO (esto debe lanzar la excepción correcta)
        BusinessException exception = assertThrows(BusinessException.class,
                () -> eventoService.publicarEvento(borrador.getIdEvento())); // Intentar publicar de nuevo

        // Verificar que la excepción sea la esperada (estado incorrecto)
        assertTrue(exception.getMessage().contains("Solo se pueden publicar eventos en estado BORRADOR"),
                "La excepción esperada era sobre el estado, pero fue: " + exception.getMessage());
    }

    @Test
    void testPublicarEvento_FallaSiNoTieneEntradas() {
        // Arrange - Crear evento BORRADOR (SIN TICKETS)
        EventoCreateDTO dtoCrear = new EventoCreateDTO();
        dtoCrear.setNombre("Evento Sin Entradas Test");
        dtoCrear.setFechaEvento(LocalDate.now().plusMonths(1));
        dtoCrear.setTipoEvento(TipoEvento.ROCK);
        dtoCrear.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO eventoBorrador = eventoService.crear(dtoCrear);

        // Act & Assert - Intentar publicar
        BusinessException exception = assertThrows(BusinessException.class,
                () -> eventoService.publicarEvento(eventoBorrador.getIdEvento()));
        assertTrue(exception.getMessage().contains("No se ha definido el mínimo de categorías de entradas"));
    }
}