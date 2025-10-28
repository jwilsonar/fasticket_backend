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
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.geografia.Departamento;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.geografia.Provincia;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.geografia.DepartamentoRepository;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.geografia.ProvinciaRepository;

/**
 * Tests de integración para LocalController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
public class LocalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocalesRepositorio localRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    private Local localTest;
    private Distrito distritoTest;

    @BeforeEach
    void setUp() {
        // Crear departamento de prueba
        Departamento departamento = new Departamento();
        departamento.setNombre("Lima");
        departamento.setActivo(true);
        departamento = departamentoRepository.save(departamento);

        // Crear provincia de prueba
        Provincia provincia = new Provincia();
        provincia.setNombre("Lima");
        provincia.setDepartamento(departamento);
        provincia.setActivo(true);
        provincia = provinciaRepository.save(provincia);

        // Crear distrito de prueba
        Distrito distrito = new Distrito();
        distrito.setNombre("Lima Cercado");
        distrito.setProvincia(provincia);
        distrito.setActivo(true);
        distritoTest = distritoRepository.save(distrito);

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
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testObtenerLocalPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/locales/" + localTest.getIdLocal()))
                .andExpect(status().isOk())
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
                .andExpect(jsonPath("$.data.nombre").value("Coliseo Nuevo"))
                .andExpect(jsonPath("$.data.aforoTotal").value(8000));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearLocal_ConImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen de local de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "local.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/locales/con-imagen")
                        .file(imagen)
                        .param("nombre", "Coliseo Con Imagen")
                        .param("aforoTotal", "10000")
                        .param("idDistrito", distritoTest.getIdDistrito().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Coliseo Con Imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
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
                .andExpect(jsonPath("$.data.nombre").value("Estadio Actualizado"))
                .andExpect(jsonPath("$.data.aforoTotal").value(12000));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarLocal_ConImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen actualizada de local".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "local_updated.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/locales/" + localTest.getIdLocal() + "/con-imagen")
                        .file(imagen)
                        .param("nombre", "Estadio Actualizado Con Imagen")
                        .param("aforoTotal", "15000")
                        .param("idDistrito", distritoTest.getIdDistrito().toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Estadio Actualizado Con Imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarLocal_Exitoso() throws Exception {
        mockMvc.perform(delete("/api/v1/locales/" + localTest.getIdLocal()))
                .andExpect(status().isOk());
    }
}

