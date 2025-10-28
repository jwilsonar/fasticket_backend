package pe.edu.pucp.fasticket.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.model.eventos.Local;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class EventoMapperTest {

    private EventoMapper eventoMapper;
    private Local localTest;

    @BeforeEach
    void setUp() {
        eventoMapper = new EventoMapper();
        localTest = new Local();
        localTest.setIdLocal(1);
        localTest.setNombre("Estadio Nacional");
    }

    @Test
    void testToResponseDTO_MapeoCorrecto() {
        // Arrange
        Evento evento = new Evento();
        evento.setIdEvento(10);
        evento.setNombre("Concierto de Prueba");
        evento.setDescripcion("Evento de prueba");
        evento.setFechaEvento(LocalDate.of(2025, 12, 31));
        evento.setHoraInicio(LocalTime.of(20, 0));
        evento.setHoraFin(LocalTime.of(23, 0));
        evento.setImagenUrl("https://example.com/img.jpg");
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setEstadoEvento(EstadoEvento.ACTIVO);
        evento.setAforoDisponible(5000);
        evento.setActivo(true);
        evento.setLocal(localTest);
        evento.setFechaCreacion(LocalDate.now());

        // Act
        EventoResponseDTO dto = eventoMapper.toResponseDTO(evento);

        // Assert
        assertNotNull(dto);
        assertEquals(10, dto.getIdEvento());
        assertEquals("Concierto de Prueba", dto.getNombre());
        assertEquals("Evento de prueba", dto.getDescripcion());
        assertEquals(TipoEvento.ROCK, dto.getTipoEvento());
        assertEquals("Estadio Nacional", dto.getNombreLocal());
        assertEquals(1, dto.getIdLocal());
    }

    @Test
    void testToResponseDTO_NullDevuelveNull() {
        assertNull(eventoMapper.toResponseDTO(null));
    }

    @Test
    void testToEntity_MapeoCorrecto() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Concierto Rock 2025");
        dto.setDescripcion("Gran concierto");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setHoraInicio(LocalTime.of(18, 0));
        dto.setHoraFin(LocalTime.of(22, 0));
        dto.setImagenUrl("https://example.com/rock.jpg");
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setEstadoEvento(EstadoEvento.ACTIVO);
        dto.setAforoDisponible(10000);

        // Act
        Evento evento = eventoMapper.toEntity(dto, localTest);

        // Assert
        assertNotNull(evento);
        assertEquals("Concierto Rock 2025", evento.getNombre());
        assertEquals(TipoEvento.ROCK, evento.getTipoEvento());
        assertEquals(localTest, evento.getLocal());
        assertEquals(EstadoEvento.ACTIVO, evento.getEstadoEvento());
        assertTrue(evento.getActivo());
        assertNotNull(evento.getFechaCreacion());
    }

    @Test
    void testToEntity_EstadoPorDefectoActivo() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento sin estado");
        dto.setFechaEvento(LocalDate.now().plusDays(10));
        dto.setTipoEvento(TipoEvento.POP);
        dto.setAforoDisponible(2000);

        // Act
        Evento evento = eventoMapper.toEntity(dto, localTest);

        // Assert
        assertEquals(EstadoEvento.ACTIVO, evento.getEstadoEvento());
    }

    @Test
    void testUpdateEntity_ActualizaCamposCorrectamente() {
        // Arrange
        Evento evento = new Evento();
        evento.setNombre("Antiguo");
        evento.setDescripcion("Viejo");
        evento.setFechaEvento(LocalDate.now().plusDays(5));
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setEstadoEvento(EstadoEvento.BORRADOR);
        evento.setLocal(localTest);

        Local nuevoLocal = new Local();
        nuevoLocal.setIdLocal(2);
        nuevoLocal.setNombre("Nuevo Estadio");

        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Nuevo Nombre");
        dto.setDescripcion("Nueva descripción");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setHoraInicio(LocalTime.of(19, 0));
        dto.setHoraFin(LocalTime.of(23, 30));
        dto.setImagenUrl("https://new-image.com");
        dto.setTipoEvento(TipoEvento.POP);
        dto.setEstadoEvento(EstadoEvento.ACTIVO);
        dto.setAforoDisponible(8000);

        // Act
        eventoMapper.updateEntity(evento, dto, nuevoLocal);

        // Assert
        assertEquals("Nuevo Nombre", evento.getNombre());
        assertEquals("Nueva descripción", evento.getDescripcion());
        assertEquals(TipoEvento.POP, evento.getTipoEvento());
        assertEquals(EstadoEvento.ACTIVO, evento.getEstadoEvento());
        assertEquals(8000, evento.getAforoDisponible());
        assertEquals(nuevoLocal, evento.getLocal());
        assertNotNull(evento.getFechaActualizacion());
    }

    @Test
    void testUpdateEntity_NoCambiaEstadoSiDTOEsNulo() {
        // Arrange
        Evento evento = new Evento();
        evento.setEstadoEvento(EstadoEvento.BORRADOR);
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Sin Estado");
        dto.setFechaEvento(LocalDate.now().plusDays(5));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setAforoDisponible(1000);

        // Act
        eventoMapper.updateEntity(evento, dto, localTest);

        // Assert
        assertEquals(EstadoEvento.BORRADOR, evento.getEstadoEvento());
    }
}
