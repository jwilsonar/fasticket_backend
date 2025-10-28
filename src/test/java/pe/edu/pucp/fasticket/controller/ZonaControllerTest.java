package pe.edu.pucp.fasticket.controller;

import java.time.LocalDate;

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
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository;

/**
 * Tests de integración para ZonaController.
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
    private ZonaRepository zonaRepository;

    @Autowired
    private LocalesRepositorio localRepository;

    private Local localTest;
    private Zona zonaTest;

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

        // Crear zona de prueba
        Zona zona = new Zona();
        zona.setNombre("VIP");
        zona.setAforoMax(100);
        zona.setActivo(true);
        zona.setLocal(localTest);
        zona.setFechaCreacion(LocalDate.now());
        zonaTest = zonaRepository.save(zona);
    }

    @Test
    void testListarZonas_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nombre").value("VIP"))
                .andExpect(jsonPath("$.data[0].aforoMax").value(100))
                .andExpect(jsonPath("$.data[0].idLocal").value(localTest.getIdLocal()));
    }

    @Test
    void testListarZonas_FiltroPorLocal() throws Exception {
        // Crear otra zona en el mismo local
        Zona zona2 = new Zona();
        zona2.setNombre("General");
        zona2.setAforoMax(200);
        zona2.setActivo(true);
        zona2.setLocal(localTest);
        zona2.setFechaCreacion(LocalDate.now());
        zonaRepository.save(zona2);

        // Crear otro local con zona
        Local local2 = new Local();
        local2.setNombre("Coliseo Test");
        local2.setAforoTotal(5000);
        local2.setActivo(true);
        local2.setFechaCreacion(LocalDate.now());
        Local local2Saved = localRepository.save(local2);

        Zona zona3 = new Zona();
        zona3.setNombre("Platea");
        zona3.setAforoMax(150);
        zona3.setActivo(true);
        zona3.setLocal(local2Saved);
        zona3.setFechaCreacion(LocalDate.now());
        zonaRepository.save(zona3);

        // Test filtrar por local específico
        mockMvc.perform(get("/api/v1/zonas?local=" + localTest.getIdLocal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].idLocal").value(localTest.getIdLocal()))
                .andExpect(jsonPath("$.data[1].idLocal").value(localTest.getIdLocal()));

        // Test filtrar por otro local
        mockMvc.perform(get("/api/v1/zonas?local=" + local2Saved.getIdLocal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Platea"))
                .andExpect(jsonPath("$.data[0].idLocal").value(local2Saved.getIdLocal()));
    }

    @Test
    void testObtenerZonaPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/zonas/" + zonaTest.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.idZona").value(zonaTest.getIdZona()))
                .andExpect(jsonPath("$.data.nombre").value("VIP"))
                .andExpect(jsonPath("$.data.aforoMax").value(100))
                .andExpect(jsonPath("$.data.idLocal").value(localTest.getIdLocal()));
    }

    @Test
    void testObtenerZonaPorId_NoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/zonas/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearZona_SinPermisoCliente() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("Nueva Zona");
        dto.setAforoMax(150);
        dto.setIdLocal(localTest.getIdLocal());

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
        dto.setIdLocal(localTest.getIdLocal());

        mockMvc.perform(post("/api/v1/zonas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Zona Premium"))
                .andExpect(jsonPath("$.data.aforoMax").value(50))
                .andExpect(jsonPath("$.data.idLocal").value(localTest.getIdLocal()))
                .andExpect(jsonPath("$.data.activo").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_ConImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen de zona de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "zona.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/zonas/con-imagen")
                        .file(imagen)
                        .param("nombre", "Zona Con Imagen")
                        .param("aforoMax", "100")
                        .param("idLocal", localTest.getIdLocal().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Zona Con Imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_LocalNoExiste() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("Zona Test");
        dto.setAforoMax(100);
        dto.setIdLocal(99999); // Local que no existe

        mockMvc.perform(post("/api/v1/zonas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearZona_DatosInvalidos() throws Exception {
        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre(""); // Nombre vacío
        dto.setAforoMax(-10); // Aforo negativo
        dto.setIdLocal(localTest.getIdLocal());

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
        dto.setIdLocal(localTest.getIdLocal());

        mockMvc.perform(put("/api/v1/zonas/" + zonaTest.getIdZona())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("VIP Actualizado"))
                .andExpect(jsonPath("$.data.aforoMax").value(150))
                .andExpect(jsonPath("$.data.idLocal").value(localTest.getIdLocal()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarZona_ConImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen actualizada de zona".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "zona_updated.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/zonas/" + zonaTest.getIdZona() + "/con-imagen")
                        .file(imagen)
                        .param("nombre", "VIP Actualizado Con Imagen")
                        .param("aforoMax", "200")
                        .param("idLocal", localTest.getIdLocal().toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("VIP Actualizado Con Imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarZona_CambiarLocal() throws Exception {
        // Crear otro local
        Local local2 = new Local();
        local2.setNombre("Coliseo Test 2");
        local2.setAforoTotal(5000);
        local2.setActivo(true);
        local2.setFechaCreacion(LocalDate.now());
        Local local2Saved = localRepository.save(local2);

        ZonaCreateDTO dto = new ZonaCreateDTO();
        dto.setNombre("VIP Movido");
        dto.setAforoMax(100);
        dto.setIdLocal(local2Saved.getIdLocal());

        mockMvc.perform(put("/api/v1/zonas/" + zonaTest.getIdZona())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("VIP Movido"))
                .andExpect(jsonPath("$.data.idLocal").value(local2Saved.getIdLocal()));
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
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarZona_NoExiste() throws Exception {
        mockMvc.perform(delete("/api/v1/zonas/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListarZonas_SinParametroLocal() throws Exception {
        // Test que sin parámetro local devuelve todas las zonas
        mockMvc.perform(get("/api/v1/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Zonas obtenidas exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testListarZonas_ConParametroLocal() throws Exception {
        // Test que con parámetro local devuelve solo las zonas de ese local
        mockMvc.perform(get("/api/v1/zonas?local=" + localTest.getIdLocal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Zonas del local " + localTest.getIdLocal() + " obtenidas exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testListarZonas_LocalSinZonas() throws Exception {
        // Crear un local sin zonas
        Local localSinZonas = new Local();
        localSinZonas.setNombre("Local Vacío");
        localSinZonas.setAforoTotal(1000);
        localSinZonas.setActivo(true);
        localSinZonas.setFechaCreacion(LocalDate.now());
        Local localSinZonasSaved = localRepository.save(localSinZonas);

        mockMvc.perform(get("/api/v1/zonas?local=" + localSinZonasSaved.getIdLocal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
