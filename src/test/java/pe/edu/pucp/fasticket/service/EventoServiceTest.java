package pe.edu.pucp.fasticket.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.services.eventos.EventoService;

/**
 * Tests para EventoService.
 * Valida operaciones CRUD de eventos.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
public class EventoServiceTest {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private LocalesRepositorio localRepository;

    private Local localTest;

    @BeforeEach
    void setUp() {
        // Crear local de prueba
        Local local = new Local();
        local.setNombre("Estadio Test");
        local.setDireccion("Av. Test 123");
        local.setAforoTotal(10000);
        local.setActivo(true);
        local.setFechaCreacion(LocalDate.now());
        localTest = localRepository.save(local);
    }

    @Test
    void testCrearEvento_Exitoso() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Concierto Rock");
        dto.setDescripcion("Gran concierto de rock");
        dto.setFechaEvento(LocalDate.now().plusMonths(2));
        dto.setHoraInicio(LocalTime.of(20, 0));
        dto.setHoraFin(LocalTime.of(23, 0));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setEstadoEvento(EstadoEvento.ACTIVO);
        dto.setAforoDisponible(5000);
        dto.setIdLocal(localTest.getIdLocal());

        // Act
        EventoResponseDTO response = eventoService.crear(dto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getIdEvento());
        assertEquals("Concierto Rock", response.getNombre());
        assertEquals(TipoEvento.ROCK, response.getTipoEvento());
        assertEquals(EstadoEvento.ACTIVO, response.getEstadoEvento());
        assertTrue(response.getActivo());
    }

    @Test
    void testCrearEvento_FechaPasada() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Pasado");
        dto.setFechaEvento(LocalDate.now().minusDays(1)); // Fecha pasada
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setAforoDisponible(1000);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> eventoService.crear(dto));
        assertTrue(exception.getMessage().contains("debe ser futura"));
    }

    @Test
    void testListarEventosActivos() {
        // Arrange - Crear eventos
        EventoCreateDTO dto1 = new EventoCreateDTO();
        dto1.setNombre("Evento 1");
        dto1.setFechaEvento(LocalDate.now().plusDays(10));
        dto1.setTipoEvento(TipoEvento.ROCK);
        dto1.setAforoDisponible(1000);
        eventoService.crear(dto1);

        EventoCreateDTO dto2 = new EventoCreateDTO();
        dto2.setNombre("Evento 2");
        dto2.setFechaEvento(LocalDate.now().plusDays(20));
        dto2.setTipoEvento(TipoEvento.POP);
        dto2.setAforoDisponible(500);
        eventoService.crear(dto2);

        // Act
        List<EventoResponseDTO> eventos = eventoService.listarActivos();

        // Assert
        assertTrue(eventos.size() >= 2);
    }

    @Test
    void testListarEventosProximos() {
        // Arrange - Crear evento futuro
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Próximo");
        dto.setFechaEvento(LocalDate.now().plusDays(5));
        dto.setTipoEvento(TipoEvento.ELECTRONICA);
        dto.setAforoDisponible(2000);
        eventoService.crear(dto);

        // Act
        List<EventoResponseDTO> eventosProximos = eventoService.listarProximos();

        // Assert
        assertFalse(eventosProximos.isEmpty());
    }

    @Test
    void testActualizarEvento_Exitoso() {
        // Arrange - Crear evento
        EventoCreateDTO dtoCrear = new EventoCreateDTO();
        dtoCrear.setNombre("Evento Original");
        dtoCrear.setFechaEvento(LocalDate.now().plusMonths(1));
        dtoCrear.setTipoEvento(TipoEvento.ROCK);
        dtoCrear.setAforoDisponible(1000);
        EventoResponseDTO eventoCreado = eventoService.crear(dtoCrear);

        // Preparar actualización
        EventoCreateDTO dtoActualizar = new EventoCreateDTO();
        dtoActualizar.setNombre("Evento Actualizado");
        dtoActualizar.setFechaEvento(LocalDate.now().plusMonths(2));
        dtoActualizar.setTipoEvento(TipoEvento.POP);
        dtoActualizar.setAforoDisponible(1500);

        // Act
        EventoResponseDTO eventoActualizado = eventoService.actualizar(eventoCreado.getIdEvento(), dtoActualizar);

        // Assert
        assertEquals("Evento Actualizado", eventoActualizado.getNombre());
        assertEquals(TipoEvento.POP, eventoActualizado.getTipoEvento());
        assertEquals(1500, eventoActualizado.getAforoDisponible());
    }

    @Test
    void testObtenerEventoPorId_NoExiste() {
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> eventoService.obtenerPorId(99999));
        assertTrue(exception.getMessage().contains("Evento no encontrado"));
    }

    @Test
    void testEliminarEvento_Logico() {
        // Arrange - Crear evento
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento a Eliminar");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setAforoDisponible(1000);
        EventoResponseDTO evento = eventoService.crear(dto);

        // Act
        eventoService.eliminarLogico(evento.getIdEvento());

        // Assert - El evento debe estar inactivo
        EventoResponseDTO eventoConsultado = eventoService.obtenerPorId(evento.getIdEvento());
        assertFalse(eventoConsultado.getActivo());
    }

    @Test
    void testListarEventosPorEstado() {
        // Arrange - Crear eventos con diferentes estados
        EventoCreateDTO dtoActivo = new EventoCreateDTO();
        dtoActivo.setNombre("Evento Activo");
        dtoActivo.setFechaEvento(LocalDate.now().plusDays(15));
        dtoActivo.setTipoEvento(TipoEvento.ROCK);
        dtoActivo.setEstadoEvento(EstadoEvento.ACTIVO);
        dtoActivo.setAforoDisponible(1000);
        eventoService.crear(dtoActivo);

        // Act
        List<EventoResponseDTO> eventosActivos = eventoService.listarPorEstado(EstadoEvento.ACTIVO);

        // Assert
        assertFalse(eventosActivos.isEmpty());
        assertTrue(eventosActivos.stream()
                .allMatch(e -> e.getEstadoEvento() == EstadoEvento.ACTIVO));
    }
}

