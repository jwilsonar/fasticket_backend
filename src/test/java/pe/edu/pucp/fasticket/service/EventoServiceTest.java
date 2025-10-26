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
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepositorio;
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
        EventoCreateDTO dtoBorrador = new EventoCreateDTO();
        dtoBorrador.setNombre("Evento Borrador Listar Activos");
        dtoBorrador.setFechaEvento(LocalDate.now().plusDays(10));
        dtoBorrador.setTipoEvento(TipoEvento.ROCK); // Use Enum
        dtoBorrador.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO borrador = eventoService.crear(dtoBorrador);

        // Arrange - Crear y PUBLICAR otro evento
        EventoCreateDTO dtoPublicado = new EventoCreateDTO();
        dtoPublicado.setNombre("Evento Publicado Listar Activos");
        dtoPublicado.setFechaEvento(LocalDate.now().plusDays(20));
        dtoPublicado.setTipoEvento(TipoEvento.POP); // Use Enum
        dtoPublicado.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO publicadoBorrador = eventoService.crear(dtoPublicado);

        // --- CORRECCIÓN: Simular adición de Ticket ANTES de publicar ---
        // 1. Obtener entidad
        Evento eventoEntityPub = eventoRepository.findById(publicadoBorrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("Setup failed: Evento borrador (para publicar) no encontrado"));
        // 2. Crear y asociar ticket
        Ticket ticketPub = new Ticket();
        ticketPub.setPrecio(1.0);
        ticketPub.setEstado(EstadoTicket.DISPONIBLE); // Use Enum
        ticketPub.setActivo(true);
        ticketPub.setEvento(eventoEntityPub);
        // 3. Añadir a colección y guardar
        if (eventoEntityPub.getTickets() == null) {
            eventoEntityPub.setTickets(new java.util.ArrayList<>());
        }
        eventoEntityPub.getTickets().add(ticketPub);
        ticketRepository.save(ticketPub);
        // --- FIN CORRECCIÓN ---

        // 4. Publicar el evento (Ahora debería funcionar)
        EventoResponseDTO publicadoFinal = eventoService.publicarEvento(publicadoBorrador.getIdEvento());

        // Act - Listar solo los activos
        List<EventoResponseDTO> eventosActivos = eventoService.listarActivos();

        // Assert
        assertTrue(eventosActivos.stream().anyMatch(e -> e.getIdEvento().equals(publicadoFinal.getIdEvento())),
                "El evento publicado (" + publicadoFinal.getIdEvento() + ") debe estar en la lista de activos");
        assertTrue(eventosActivos.stream().noneMatch(e -> e.getIdEvento().equals(borrador.getIdEvento())),
                "El evento borrador (" + borrador.getIdEvento() + ") NO debe estar en la lista de activos");
        assertTrue(eventosActivos.stream().allMatch(EventoResponseDTO::getActivo),
                "Todos los eventos listados deben tener activo = true");
        assertTrue(eventosActivos.stream().allMatch(e -> e.getEstadoEvento() == EstadoEvento.PUBLICADO),
                "Todos los eventos listados deben tener estado PUBLICADO (o estados considerados activos)");
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

        // --- CORRECCIÓN: Simular adición de Ticket ANTES de publicar ---
        // 1. Obtener entidad
        Evento eventoEntityProx = eventoRepository.findById(publicadoBorrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("Setup failed: Evento borrador no encontrado"));
        // 2. Crear y asociar ticket
        Ticket ticketProx = new Ticket();
        ticketProx.setPrecio(1.0);
        ticketProx.setEstado(EstadoTicket.DISPONIBLE); // Use Enum
        ticketProx.setActivo(true);
        ticketProx.setEvento(eventoEntityProx);
        // 3. Añadir a colección y guardar
        if (eventoEntityProx.getTickets() == null) {
            eventoEntityProx.setTickets(new java.util.ArrayList<>());
        }
        eventoEntityProx.getTickets().add(ticketProx);
        ticketRepository.save(ticketProx);
        // --- FIN CORRECCIÓN ---

        // 4. Publicar el evento (Ahora debería funcionar)
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
        assertTrue(eventosProximos.stream().allMatch(e -> e.getEstadoEvento() == EstadoEvento.PUBLICADO),
                "Todos los eventos listados deben estar publicados");
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
        dtoCrear.setTipoEvento(TipoEvento.ROCK); // Asegúrate que TipoEvento sea tu Enum/Clase
        dtoCrear.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO eventoBorrador = eventoService.crear(dtoCrear);

        // --- CORRECCIÓN: Añadir Ticket a la colección del Evento ---
        // 1. Obtener la entidad Evento recién creada
        Evento eventoEntity = eventoRepository.findById(eventoBorrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("El evento borrador no se guardó correctamente"));

        // 2. Crear el Ticket de prueba
        Ticket ticketDePrueba = new Ticket();
        ticketDePrueba.setPrecio(10.0);
        ticketDePrueba.setEstado(EstadoTicket.DISPONIBLE); // Usa tu Enum EstadoTicket
        ticketDePrueba.setActivo(true);
        ticketDePrueba.setEvento(eventoEntity); // Asocia el ticket al evento

        // 3. ¡IMPORTANTE! Añadir el ticket a la colección del evento
        // Esto asume que tienes inicializada la lista en Evento.java (ej. new ArrayList<>())
        if (eventoEntity.getTickets() == null) {
            eventoEntity.setTickets(new java.util.ArrayList<>()); // Inicializar si es null
        }
        eventoEntity.getTickets().add(ticketDePrueba);

        // 4. Guardar el Evento (si tienes CascadeType.PERSIST o ALL en Evento.tickets)
        // O guardar el Ticket (si la relación la maneja el Ticket)
        // Guardaremos el ticket explícitamente para asegurar
        ticketRepository.save(ticketDePrueba);
        // Opcional: Podrías necesitar guardar el evento de nuevo si Cascade no está configurado
        // eventoRepository.save(eventoEntity);
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
        dto.setTipoEvento(TipoEvento.ROCK); // Use your Enum/Class
        dto.setIdLocal(localTest.getIdLocal());
        EventoResponseDTO borrador = eventoService.crear(dto);

        // --- CORRECCIÓN: Asegurar que el ticket se asocia correctamente ANTES de la 1ra publicación ---
        // 1. Obtener la entidad Evento
        Evento eventoEntityPub = eventoRepository.findById(borrador.getIdEvento())
                .orElseThrow(() -> new AssertionError("Setup failed: Evento borrador no encontrado"));

        // 2. Crear y asociar el Ticket
        Ticket ticketPub = new Ticket();
        ticketPub.setPrecio(1.0);
        ticketPub.setEstado(EstadoTicket.DISPONIBLE); // Use your Enum
        ticketPub.setActivo(true);
        ticketPub.setEvento(eventoEntityPub); // Asocia al evento

        // 3. Añadir a la colección y guardar
        if (eventoEntityPub.getTickets() == null) {
            eventoEntityPub.setTickets(new java.util.ArrayList<>());
        }
        eventoEntityPub.getTickets().add(ticketPub);
        ticketRepository.save(ticketPub); // Guardar explícitamente el ticket
        // Opcional: eventoRepository.save(eventoEntityPub); si la cascada no está bien configurada
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