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
        // Instanciación directa ya que EventoMapper no tiene dependencias inyectadas
        eventoMapper = new EventoMapper(); 
        localTest = new Local();
        localTest.setIdLocal(1);
        localTest.setNombre("Estadio Nacional");
    }

    // -------------------------------------------------------------------------
    // --- Mapeo Entidad -> DTO (Respuesta) ---
    // -------------------------------------------------------------------------
    
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
        evento.setImagenZonasUrl("https://example.com/zones.jpg");
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
        assertEquals("https://example.com/img.jpg", dto.getImagenUrl(), "Debe mapear la URL principal.");
        assertEquals("https://example.com/zones.jpg", dto.getImagenZonasUrl(), "Debe mapear la URL de zonas.");
    }

    @Test
    void testToResponseDTO_NullDevuelveNull() {
        assertNull(eventoMapper.toResponseDTO(null));
    }

    // -------------------------------------------------------------------------
    // --- Mapeo DTO -> Entidad (Creación) ---
    // -------------------------------------------------------------------------

    @Test
    void testToEntity_MapeoCorrecto() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Concierto Rock 2025");
        dto.setDescripcion("Gran concierto");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setHoraInicio(LocalTime.of(18, 0));
        dto.setHoraFin(LocalTime.of(22, 0));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setEstadoEvento(EstadoEvento.ACTIVO);
        dto.setAforoDisponible(10000);
        dto.setMenoresDeEdadPermitidos(false);
        dto.setRestricciones("Solo mayores de edad");
        
        // Act
        Evento evento = eventoMapper.toEntity(dto, localTest);

        // Assert
        assertNotNull(evento);
        assertEquals("Concierto Rock 2025", evento.getNombre());
        assertEquals(TipoEvento.ROCK, evento.getTipoEvento());
        assertEquals(localTest, evento.getLocal());
        assertEquals(EstadoEvento.ACTIVO, evento.getEstadoEvento());
        assertTrue(evento.getActivo(), "El evento debe estar activo por defecto.");
        assertNotNull(evento.getFechaCreacion());
        assertNull(evento.getImagenUrl(), "Las URLs deben ser null, el mapper no las establece.");
        assertEquals(false, evento.getMenoresDeEdadPermitidos());
    }

    @Test
    void testToEntity_EstadoPorDefectoActivo() {
        // Arrange
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento sin estado");
        dto.setFechaEvento(LocalDate.now().plusDays(10));
        dto.setTipoEvento(TipoEvento.POP);
        dto.setAforoDisponible(2000);
        // EstadoEvento = null en el DTO

        // Act
        Evento evento = eventoMapper.toEntity(dto, localTest);

        // Assert
        // El mapper aplica EstadoEvento.ACTIVO si el DTO lo trae null
        assertEquals(EstadoEvento.ACTIVO, evento.getEstadoEvento(), "Debe aplicar el estado ACTIVO por defecto.");
    }

    // -------------------------------------------------------------------------
    // --- Actualización de Entidad (Update) ---
    // -------------------------------------------------------------------------
    
    @Test
    void testUpdateEntity_ActualizaCamposCorrectamente() {
        // Arrange
        Evento evento = new Evento();
        evento.setNombre("Antiguo");
        evento.setImagenUrl("https://old-image.com"); // URL Antigua (debe mantenerse)
        evento.setImagenZonasUrl("https://old-zones.com"); // URL Antigua de Zonas (debe mantenerse)
        evento.setEstadoEvento(EstadoEvento.BORRADOR);

        Local nuevoLocal = new Local();
        nuevoLocal.setIdLocal(2);
        nuevoLocal.setNombre("Nuevo Estadio");

        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Nuevo Nombre");
        dto.setDescripcion("Nueva descripción");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setHoraInicio(LocalTime.of(19, 0));
        dto.setTipoEvento(TipoEvento.POP);
        dto.setEstadoEvento(EstadoEvento.ACTIVO);
        dto.setAforoDisponible(8000);
        
        // Nota: Las URLs en el DTO son ignoradas por el mapper, por lo que las seteamos a null.
        dto.setImagenUrl(null); 
        dto.setImagenZonasUrl(null); 

        // Act
        eventoMapper.updateEntity(evento, dto, nuevoLocal);

        // Assert
        assertEquals("Nuevo Nombre", evento.getNombre());
        assertEquals("Nueva descripción", evento.getDescripcion());
        assertEquals(TipoEvento.POP, evento.getTipoEvento());
        assertEquals(EstadoEvento.ACTIVO, evento.getEstadoEvento());
        assertEquals(8000, evento.getAforoDisponible());
        assertEquals(nuevoLocal, evento.getLocal());
        
        // ASUNCIÓN CLAVE: El mapper ignora las URLs, por lo que DEBEN MANTENERSE las antiguas.
        assertEquals("https://old-image.com", evento.getImagenUrl(), "La URL de imagen debe mantenerse (el mapper la ignora).");
        assertEquals("https://old-zones.com", evento.getImagenZonasUrl(), "La URL de zonas debe mantenerse (el mapper la ignora).");
        
        assertNotNull(evento.getFechaActualizacion());
    }

    @Test
    void testUpdateEntity_NoSobreescribeConNullSiElDtoEsParcial() {
        // Arrange
        Evento evento = new Evento();
        evento.setNombre("Nombre Anterior");
        evento.setDescripcion("Descripción Original");
        evento.setEstadoEvento(EstadoEvento.BORRADOR);
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setAforoDisponible(1000);
        evento.setMenoresDeEdadPermitidos(true);

        // DTO parcial: solo actualiza el nombre
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Solo Cambia Nombre");
        // El resto de campos del DTO son null

        // Act
        eventoMapper.updateEntity(evento, dto, localTest);

        // Assert
        assertEquals("Solo Cambia Nombre", evento.getNombre());
        // Estos campos no deben ser sobreescritos por null
        assertEquals("Descripción Original", evento.getDescripcion());
        assertEquals(TipoEvento.ROCK, evento.getTipoEvento());
        assertEquals(EstadoEvento.BORRADOR, evento.getEstadoEvento());
        assertEquals(1000, evento.getAforoDisponible());
        assertEquals(true, evento.getMenoresDeEdadPermitidos());
    }
}