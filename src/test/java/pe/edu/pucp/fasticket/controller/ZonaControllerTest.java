package pe.edu.pucp.fasticket.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository; // <-- Repo correcto

/**
 * Tests de integración para ZonaController (Actualizado para Evento -> Zona).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
public class ZonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZonaRepository zonaRepository; // <-- Repo correcto

    @Autowired
    private LocalesRepositorio localRepository;

    @Autowired
    private EventosRepositorio eventoRepository; // <-- Añadido

    private Local localTest;
    private Evento eventoTest; // <-- Añadido
    private Zona zonaTest;

    @BeforeEach
    void setUp() {
        // 1. Crear local de prueba
        localTest = new Local();
        localTest.setNombre("Estadio Test");
        localTest.setDireccion("Av. Test 123");
        localTest.setAforoTotal(10000);
        localTest.setActivo(true);
        localTest.setFechaCreacion(LocalDate.now());
        localTest = localRepository.save(localTest);

        // 2. Crear evento de prueba
        eventoTest = new Evento();
        eventoTest.setNombre("Evento Test");
        eventoTest.setFechaEvento(LocalDate.now().plusMonths(1));
        eventoTest.setHoraInicio(LocalTime.of(20, 0));
        eventoTest.setEstadoEvento(EstadoEvento.PUBLICADO);
        eventoTest.setActivo(true);
        eventoTest.setLocal(localTest);
        eventoTest = eventoRepository.save(eventoTest);

        // 3. Crear zona de prueba (Asignada al Evento)
        zonaTest = new Zona();
        zonaTest.setNombre("VIP");
        zonaTest.setAforoMax(100);
        zonaTest.setActivo(true);
        zonaTest.setEvento(eventoTest); // <-- ASIGNADO A EVENTO
        zonaTest.setFechaCreacion(LocalDate.now());
        zonaTest = zonaRepository.save(zonaTest);
    }

    @AfterEach
    void tearDown() {
        // Limpiar en orden inverso
        zonaRepository.deleteAll();
        eventoRepository.deleteAll();
        localRepository.deleteAll();
    }


    // --- TESTS DE ENDPOINTS PÚBLICOS ---

    @Test
    void testListarZonas_FiltroPorEvento() throws Exception {
        // 1. Crear otra zona en el mismo evento (Evento 1)
        Zona zona2 = new Zona();
        zona2.setNombre("General");
        zona2.setAforoMax(200);
        zona2.setActivo(true);
        zona2.setEvento(eventoTest); // Asignado a Evento 1
        zona2.setFechaCreacion(LocalDate.now());
        zonaRepository.save(zona2);

        // 2. Crear otro evento (Evento 2) en el mismo local
        Evento evento2 = new Evento();
        evento2.setNombre("Evento Test 2");
        evento2.setFechaEvento(LocalDate.now().plusMonths(2));
        evento2.setHoraInicio(LocalTime.of(20, 0));
        evento2.setEstadoEvento(EstadoEvento.PUBLICADO);
        evento2.setActivo(true);
        evento2.setLocal(localTest);
        evento2 = eventoRepository.save(evento2);

        Zona zona3 = new Zona();
        zona3.setNombre("Platea");
        zona3.setAforoMax(150);
        zona3.setActivo(true);
        zona3.setEvento(evento2); // Asignado a Evento 2
        zona3.setFechaCreacion(LocalDate.now());
        zonaRepository.save(zona3);

        // 3. Test: filtrar por Evento 1 (debe devolver 2 zonas)
        mockMvc.perform(get("/api/v1/zonas?evento=" + eventoTest.getIdEvento())) // <-- Usa ?evento=
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.mensaje").value("Zonas del evento " + eventoTest.getIdEvento() + " obtenidas exitosamente"));

        // 4. Test: filtrar por Evento 2 (debe devolver 1 zona)
        mockMvc.perform(get("/api/v1/zonas?evento=" + evento2.getIdEvento())) // <-- Usa ?evento=
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Platea"))
                .andExpect(jsonPath("$.data[0].idEvento").value(evento2.getIdEvento()));
    }

    @Test
    void testObtenerZonaPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/zonas/" + zonaTest.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.idZona").value(zonaTest.getIdZona()))
                .andExpect(jsonPath("$.data.nombre").value("VIP"))
                .andExpect(jsonPath("$.data.aforoMax").value(100))
                .andExpect(jsonPath("$.data.idEvento").value(eventoTest.getIdEvento())); // <-- Corregido
    }

    @Test
    void testObtenerZonaPorId_NoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/zonas/99999"))
                .andExpect(status().isNotFound());
    }

    // --- TESTS DE ENDPOINTS DE ADMIN ---

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearZona_SinPermisoCliente() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("Nueva Zona");
        dto.setAforoMax(150);
        dto.setIdEvento(eventoTest.getIdEvento()); // <-- Corregido

        mockMvc.perform(post("/api/v1/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_ConPermisoAdmin() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("Zona Premium");
        dto.setAforoMax(50);
        dto.setIdEvento(eventoTest.getIdEvento()); // <-- Corregido

        mockMvc.perform(post("/api/v1/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Zona Premium"))
                .andExpect(jsonPath("$.data.aforoMax").value(50))
                .andExpect(jsonPath("$.data.idEvento").value(eventoTest.getIdEvento())) // <-- Corregido
                .andExpect(jsonPath("$.data.activo").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_ConImagen() throws Exception {
        byte[] imagenBytes = "imagen de zona de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen =
                new org.springframework.mock.web.MockMultipartFile("imagen", "zona.jpg", "image/jpeg", imagenBytes);

        String response = mockMvc.perform(multipart("/api/v1/zonas/con-imagen")
                        .file(imagen)
                        .param("nombre", "Zona Con Imagen")
                        .param("aforoMax", "100")
                        .param("idEvento", eventoTest.getIdEvento().toString())) // <-- Corregido
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Zona Con Imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists())
                .andReturn().getResponse().getContentAsString();

        Integer idZonaCreada = objectMapper.readTree(response).get("data").get("idZona").asInt();

        mockMvc.perform(get("/api/v1/zonas/" + idZonaCreada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idZona").value(idZonaCreada))
                .andExpect(jsonPath("$.data.imagenUrl").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_EventoNoExiste() throws Exception { // <-- Corregido
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("Zona Test");
        dto.setAforoMax(100);
        dto.setIdEvento(99999); // Evento que no existe

        mockMvc.perform(post("/api/v1/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()); // O 404
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_DatosInvalidos() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre(""); // Nombre vacío
        dto.setAforoMax(-10); // Aforo negativo
        dto.setIdEvento(eventoTest.getIdEvento()); // <-- Corregido

        mockMvc.perform(post("/api/v1/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarZona_Exitoso() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("VIP Actualizado");
        dto.setAforoMax(150);
        dto.setIdEvento(eventoTest.getIdEvento()); // <-- Corregido

        mockMvc.perform(put("/api/v1/zonas/" + zonaTest.getIdZona())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("VIP Actualizado"))
                .andExpect(jsonPath("$.data.aforoMax").value(150))
                .andExpect(jsonPath("$.data.idEvento").value(eventoTest.getIdEvento())); // <-- Corregido
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarZona_ConImagen() throws Exception {
        byte[] imagenBytes = "imagen actualizada de zona".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen =
                new org.springframework.mock.web.MockMultipartFile("imagen", "zona_updated.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/zonas/" + zonaTest.getIdZona() + "/con-imagen")
                        .file(imagen)
                        .param("nombre", "VIP Actualizado Con Imagen")
                        .param("aforoMax", "200")
                        .param("idEvento", eventoTest.getIdEvento().toString()) // <-- Corregido
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("VIP Actualizado Con Imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());

        mockMvc.perform(get("/api/v1/zonas/" + zonaTest.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imagenUrl").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarZona_CambiarEvento() throws Exception { // <-- Corregido
        // 1. Crear otro local
        Local local2 = new Local();
        local2.setNombre("Coliseo Test 2");
        local2.setAforoTotal(5000);
        local2.setActivo(true);
        local2.setFechaCreacion(LocalDate.now());
        Local local2Saved = localRepository.save(local2);

        // 2. Crear otro evento
        Evento evento2 = new Evento();
        evento2.setNombre("Evento 2");
        evento2.setFechaEvento(LocalDate.now().plusMonths(2));
        evento2.setHoraInicio(LocalTime.of(20, 0));
        evento2.setEstadoEvento(EstadoEvento.PUBLICADO);
        evento2.setActivo(true);
        evento2.setLocal(local2Saved);
        Evento evento2Saved = eventoRepository.save(evento2);

        // 3. DTO para mover la zona al Evento 2
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("VIP Movido");
        dto.setAforoMax(100);
        dto.setIdEvento(evento2Saved.getIdEvento()); // <-- Corregido

        mockMvc.perform(put("/api/v1/zonas/" + zonaTest.getIdZona())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("VIP Movido"))
                .andExpect(jsonPath("$.data.idEvento").value(evento2Saved.getIdEvento())); // <-- Corregido
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarZona_Exitoso() throws Exception {
        mockMvc.perform(delete("/api/v1/zonas/" + zonaTest.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Zona eliminada exitosamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR") // <-- Corregido
    void testEliminarZona_NoExiste() throws Exception {
        mockMvc.perform(delete("/api/v1/zonas/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListarZonas_EventoSinZonas() throws Exception {
        // 1. Crear un evento sin zonas
        Evento eventoSinZonas = new Evento();
        eventoSinZonas.setNombre("Evento Vacío");
        eventoSinZonas.setFechaEvento(LocalDate.now().plusMonths(1));
        eventoSinZonas.setHoraInicio(LocalTime.of(20, 0));
        eventoSinZonas.setEstadoEvento(EstadoEvento.PUBLICADO);
        eventoSinZonas.setActivo(true);
        eventoSinZonas.setLocal(localTest);
        Evento eventoSinZonasSaved = eventoRepository.save(eventoSinZonas);

        // 2. Probar el filtro
        mockMvc.perform(get("/api/v1/zonas?evento=" + eventoSinZonasSaved.getIdEvento())) // <-- Corregido
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testSubirImagenZona_YVerificarPersistencia() throws Exception {
        byte[] imagenBytes = "imagen de zona independiente".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen =
                new org.springframework.mock.web.MockMultipartFile("file", "zona_upload.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/zonas/" + zonaTest.getIdZona() + "/imagen")
                        .file(imagen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isNotEmpty());

        mockMvc.perform(get("/api/v1/zonas/" + zonaTest.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imagenUrl").isNotEmpty());
    }
}