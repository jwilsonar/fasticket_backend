package pe.edu.pucp.fasticket.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull; // Añadida
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
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
 * Valida operaciones CRUD de eventos, incluyendo el manejo de imágenes.
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
    
    // URL Fija que devuelve tu mockS3Service en TestConfig:
    // "https://test-bucket.s3.us-east-1.amazonaws.com/{folder}/{entityId}/mock-file.jpg"
    private static final String S3_MOCK_BASE_URL = "https://test-bucket.s3.us-east-1.amazonaws.com/eventos/%d/mock-file.jpg";


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

    // -------------------------------------------------------------------------
    // --- MÉTODOS AUXILIARES ---
    // -------------------------------------------------------------------------

    /** Crea un DTO base válido para evitar repetición en los tests. */
    private EventoCreateDTO crearDtoBase() {
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Base");
        dto.setDescripcion("Descripción Base");
        dto.setFechaEvento(LocalDate.now().plusMonths(3));
        dto.setHoraInicio(LocalTime.of(18, 0));
        dto.setHoraFin(LocalTime.of(22, 0));
        dto.setTipoEvento(TipoEvento.FESTIVAL);
        dto.setEstadoEvento(EstadoEvento.ACTIVO);
        dto.setAforoDisponible(5000);
        dto.setIdLocal(localTest.getIdLocal());
        dto.setMenoresDeEdadPermitidos(false);
        dto.setRestricciones("Restricciones");
        dto.setPoliticasDevolucion("Políticas");
        return dto;
    }

    /** Simula un archivo MultipartFile */
    private MockMultipartFile crearMockFile(String paramName, String fileName, String content) {
        // paramName debe coincidir con el campo del DTO (imagenUrl o imagenZonasUrl)
        return new MockMultipartFile(
            paramName, 
            fileName, 
            "image/jpeg",
            content.getBytes()
        );
    }


    // -------------------------------------------------------------------------
    // --- TESTS DE CREACIÓN (CRUD y IMÁGENES) ---
    // -------------------------------------------------------------------------

    @Test
    void testCrearEvento_Exitoso() {
        EventoCreateDTO dto = crearDtoBase();
        dto.setNombre("Concierto Rock");
        dto.setTipoEvento(TipoEvento.ROCK);
        
        EventoResponseDTO response = eventoService.crear(dto);

        assertNotNull(response);
        assertNotNull(response.getIdEvento());
        assertEquals("Concierto Rock", response.getNombre());
        assertEquals(TipoEvento.ROCK, response.getTipoEvento());
        assertEquals(EstadoEvento.ACTIVO, response.getEstadoEvento());
        assertTrue(response.getActivo());
    }

    @Test
    void testCrearEvento_FechaPasada() {
        EventoCreateDTO dto = crearDtoBase();
        dto.setNombre("Evento Pasado");
        dto.setFechaEvento(LocalDate.now().minusDays(1)); // Fecha pasada
        dto.setTipoEvento(TipoEvento.ROCK);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> eventoService.crear(dto));
        assertTrue(exception.getMessage().contains("debe ser futura"));
    }

    @Test
    void testCrearEvento_1_SinImagenes() {
        EventoCreateDTO dto = crearDtoBase();
        dto.setImagenUrl(null);
        dto.setImagenZonasUrl(null);
        
        EventoResponseDTO response = eventoService.crear(dto);

        assertNotNull(response);
        assertNull(response.getImagenUrl(), "La URL principal debe ser nula.");
        assertNull(response.getImagenZonasUrl(), "La URL de zonas debe ser nula.");

        System.out.println("Evento creado sin imágenes. ID: " + response.getIdEvento());
    }

    @Test
    void testCrearEvento_2_ConAmbasImagenes() {
        EventoCreateDTO dto = crearDtoBase();
        
        // Simular archivos
        dto.setImagenUrl(crearMockFile("imagenUrl", "principal.jpg", "contenido-principal-v1"));
        dto.setImagenZonasUrl(crearMockFile("imagenZonasUrl", "zonas.jpg", "contenido-zonas-v1"));
        
        EventoResponseDTO response = eventoService.crear(dto);
        Integer eventoId = response.getIdEvento();
        String expectedUrl = String.format(S3_MOCK_BASE_URL, eventoId); // URL Fija del mock
        
        assertNotNull(response);
        assertEquals(expectedUrl, response.getImagenUrl(), "La URL principal debe ser la URL fija del mock.");
        assertEquals(expectedUrl, response.getImagenZonasUrl(), "La URL de zonas debe ser la URL fija del mock.");

    }

    @Test
    void testCrearEvento_3_ConSoloImagenPrincipal() {
        EventoCreateDTO dto = crearDtoBase();
        dto.setImagenUrl(crearMockFile("imagenUrl", "principal.jpg", "contenido-principal"));
        dto.setImagenZonasUrl(null); 
        
        EventoResponseDTO response = eventoService.crear(dto);
        Integer eventoId = response.getIdEvento();
        String expectedUrl = String.format(S3_MOCK_BASE_URL, eventoId);
        
        assertNotNull(response);
        assertEquals(expectedUrl, response.getImagenUrl(), "La URL principal debe ser la URL fija del mock.");
        assertNull(response.getImagenZonasUrl(), "La URL de zonas debe ser nula."); 
    }

    // -------------------------------------------------------------------------
    // --- TESTS DE ACTUALIZACIÓN (METADATA y IMÁGENES) ---
    // -------------------------------------------------------------------------

    @Test
    void testActualizarEvento_Exitoso() {
        // Arrange - Crear evento (metadatos)
        EventoCreateDTO dtoCrear = crearDtoBase();
        dtoCrear.setNombre("Evento Original");
        dtoCrear.setTipoEvento(TipoEvento.ROCK);
        EventoResponseDTO eventoCreado = eventoService.crear(dtoCrear);

        // Preparar actualización (Metadata-only update)
        EventoCreateDTO dtoActualizar = new EventoCreateDTO();
        dtoActualizar.setNombre("Evento Actualizado Metadatos");
        dtoActualizar.setTipoEvento(TipoEvento.POP);
        dtoActualizar.setAforoDisponible(1500);

        // Act
        // Usamos .actualizar(id, dto) que no maneja MultipartFile
        EventoResponseDTO eventoActualizado = eventoService.actualizar(eventoCreado.getIdEvento(), dtoActualizar);

        // Assert
        assertEquals("Evento Actualizado Metadatos", eventoActualizado.getNombre());
        assertEquals(TipoEvento.POP, eventoActualizado.getTipoEvento());
        assertEquals(1500, eventoActualizado.getAforoDisponible());
    }
    
    @Test
    void testActualizarEvento_4_ReemplazarImagenExistente() {
        // 1. Crear evento con una imagen
        EventoCreateDTO dtoInicial = crearDtoBase();
        dtoInicial.setImagenUrl(crearMockFile("imagenUrl", "principal_v1.jpg", "contenido-v1"));
        EventoResponseDTO eventoCreado = eventoService.crear(dtoInicial);
        Integer eventoId = eventoCreado.getIdEvento();
        
        // La URL inicial es la URL fija del mock:
        String urlInicial = eventoCreado.getImagenUrl();
        
        // 2. Preparar DTO de Actualización (nuevo archivo principal)
        EventoCreateDTO dtoActualizar = new EventoCreateDTO();
        dtoActualizar.setNombre("Evento Reemplazo Principal");
        dtoActualizar.setImagenUrl(crearMockFile("imagenUrl", "principal_v2.jpg", "contenido-v2"));
        dtoActualizar.setImagenZonasUrl(null); 

        // 3. Act
        // Usamos .actualizarConImagen(id, dto)
        EventoResponseDTO eventoActualizado = eventoService.actualizarConImagen(eventoId, dtoActualizar);
        String expectedNewUrl = String.format(S3_MOCK_BASE_URL, eventoId);

        // 4. Assert
        assertEquals("Evento Reemplazo Principal", eventoActualizado.getNombre());
        
        // Verificamos que se haya guardado la URL fija del mock (indicando que el servicio S3 fue llamado)
        assertEquals(expectedNewUrl, eventoActualizado.getImagenUrl(), 
                    "La URL principal debe ser la URL fija del mock (se llamó al servicio S3).");
        assertNull(eventoActualizado.getImagenZonasUrl(), "La URL de zonas debe seguir siendo nula.");
    }
    
    @Test
    void testActualizarEvento_5_AgregarImagenFaltanteYDejarExistente() {
        // 1. Crear evento solo con imagen principal
        EventoCreateDTO dtoInicial = crearDtoBase();
        dtoInicial.setImagenUrl(crearMockFile("imagenUrl", "principal.jpg", "contenido-princ"));
        dtoInicial.setImagenZonasUrl(null);
        EventoResponseDTO eventoCreado = eventoService.crear(dtoInicial);
        Integer eventoId = eventoCreado.getIdEvento();
        
        String urlInicialPrincipal = eventoCreado.getImagenUrl();
        
        // 2. Preparar DTO de Actualización: agregamos zonas y dejamos principal en null.
        EventoCreateDTO dtoActualizar = new EventoCreateDTO();
        dtoActualizar.setNombre("Evento Agrega Zona");
        
        // Se envía null -> el servicio NO debe cambiar la URL existente
        dtoActualizar.setImagenUrl(null); 
        // Se sube nueva imagen de zonas
        dtoActualizar.setImagenZonasUrl(crearMockFile("imagenZonasUrl", "zonas.jpg", "contenido-zonas-v1"));

        // 3. Act
        EventoResponseDTO eventoActualizado = eventoService.actualizarConImagen(eventoId, dtoActualizar);
        String expectedZonasUrl = String.format(S3_MOCK_BASE_URL, eventoId);

        // 4. Assert
        assertEquals(urlInicialPrincipal, eventoActualizado.getImagenUrl(), 
                     "La URL principal debe mantenerse igual.");
        
        // La URL de zonas debe ser la URL fija del mock (la nueva imagen)
        assertEquals(expectedZonasUrl, eventoActualizado.getImagenZonasUrl(), "La URL de zonas debe ser la URL fija del mock.");
    }
    
    // -------------------------------------------------------------------------
    // --- TESTS DE CONSULTA Y ELIMINACIÓN ---
    // -------------------------------------------------------------------------

    @Test
    void testListarEventosActivos() {
        // Arrange - Crear eventos
        EventoCreateDTO dto1 = crearDtoBase();
        dto1.setNombre("Evento 1");
        eventoService.crear(dto1);

        EventoCreateDTO dto2 = crearDtoBase();
        dto2.setNombre("Evento 2");
        eventoService.crear(dto2);

        // Act
        List<EventoResponseDTO> eventos = eventoService.listarActivos();

        // Assert
        assertTrue(eventos.size() >= 2);
    }

    @Test
    void testListarEventosProximos() {
        // Arrange - Crear evento futuro
        EventoCreateDTO dto = crearDtoBase();
        dto.setNombre("Evento Próximo");
        dto.setFechaEvento(LocalDate.now().plusDays(5));
        eventoService.crear(dto);

        // Act
        List<EventoResponseDTO> eventosProximos = eventoService.listarProximos();

        // Assert
        assertFalse(eventosProximos.isEmpty());
    }

    @Test
    void testListarEventosPorEstado() {
        // Arrange - Crear eventos con diferentes estados
        EventoCreateDTO dtoActivo = crearDtoBase();
        dtoActivo.setNombre("Evento Activo");
        dtoActivo.setEstadoEvento(EstadoEvento.ACTIVO);
        eventoService.crear(dtoActivo);

        // Act
        List<EventoResponseDTO> eventosActivos = eventoService.listarPorEstado(EstadoEvento.ACTIVO);

        // Assert
        assertFalse(eventosActivos.isEmpty());
        assertTrue(eventosActivos.stream()
                .allMatch(e -> e.getEstadoEvento() == EstadoEvento.ACTIVO));
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
        EventoCreateDTO dto = crearDtoBase();
        dto.setNombre("Evento a Eliminar");
        EventoResponseDTO evento = eventoService.crear(dto);

        // Act
        eventoService.eliminarLogico(evento.getIdEvento());

        // Assert - El evento debe estar inactivo
        EventoResponseDTO eventoConsultado = eventoService.obtenerPorId(evento.getIdEvento());
        assertFalse(eventoConsultado.getActivo());
    }
}
