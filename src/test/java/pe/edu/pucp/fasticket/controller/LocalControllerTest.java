package pe.edu.pucp.fasticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para LocalController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class LocalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void testListarLocales_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/locales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testObtenerLocalPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/locales/" + localTest.getIdLocal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.data.idLocal").value(localTest.getIdLocal()))
                .andExpect(jsonPath("$.data.nombre").value("Estadio Test"))
                .andExpect(jsonPath("$.data.aforoTotal").value(10000));
    }

    @Test
    void testObtenerLocalPorId_NoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/locales/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearLocal_SinPermisoCliente() throws Exception {
        LocalCreateDTO dto = new LocalCreateDTO();
        dto.setNombre("Nuevo Local");
        dto.setAforoTotal(5000);

        mockMvc.perform(post("/api/v1/locales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearLocal_ConPermisoAdmin() throws Exception {
        LocalCreateDTO dto = new LocalCreateDTO();
        dto.setNombre("Coliseo Nuevo");
        dto.setDireccion("Av. Principal 456");
        dto.setAforoTotal(8000);

        mockMvc.perform(post("/api/v1/locales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Local creado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value("Coliseo Nuevo"))
                .andExpect(jsonPath("$.data.aforoTotal").value(8000));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarLocal_Exitoso() throws Exception {
        LocalCreateDTO dto = new LocalCreateDTO();
        dto.setNombre("Estadio Actualizado");
        dto.setDireccion("Nueva Dirección");
        dto.setAforoTotal(12000);

        mockMvc.perform(put("/api/v1/locales/" + localTest.getIdLocal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Local actualizado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value("Estadio Actualizado"))
                .andExpect(jsonPath("$.data.aforoTotal").value(12000));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarLocal_Exitoso() throws Exception {
        mockMvc.perform(delete("/api/v1/locales/" + localTest.getIdLocal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Local desactivado exitosamente"));
    }
}

