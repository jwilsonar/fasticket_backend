package pe.edu.pucp.fasticket.controller; // Verify package

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
// --- NECESSARY IMPORT ---
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors; // Import for csrf()
// --- END IMPORT ---
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.eventos.EventoCreateDTO;
import pe.edu.pucp.fasticket.model.eventos.*; // Import *
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class EventoControllerTest {

    // --- ADD @Autowired ---
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventosRepositorio eventoRepository;
    @Autowired
    private LocalesRepositorio localRepository;
    // --- END @Autowired ---

    private Evento eventoTestPublicado;
    private Local localTest;

    @BeforeEach
    void setUp() {
        Local local = new Local();
        local.setNombre("Estadio Controller Test");
        local.setDireccion("Av. Controller 123");
        local.setAforoTotal(10000);
        local.setActivo(true);
        localTest = localRepository.save(local);

        Evento evento = new Evento();
        evento.setNombre("Concierto Test Publicado");
        evento.setDescripcion("Descripción test");
        evento.setFechaEvento(LocalDate.now().plusMonths(1));
        evento.setHoraInicio(LocalTime.of(20, 0));
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setEstadoEvento(EstadoEvento.PUBLICADO);
        evento.setAforoDisponible(5000); // Assuming this field exists
        evento.setActivo(true);
        evento.setLocal(localTest);
        eventoTestPublicado = eventoRepository.save(evento);
    }

    @Test
    void testListarEventos_SinAutenticacion_DebeIncluirPublicado() throws Exception {
        mockMvc.perform(get("/api/v1/eventos")) // soloActivos = true por defecto
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.idEvento == %d)]", eventoTestPublicado.getIdEvento()).exists()); // Check if our published event is there
    }

    @Test
    void testObtenerEventoPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/" + eventoTestPublicado.getIdEvento()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.idEvento").value(eventoTestPublicado.getIdEvento()))
                .andExpect(jsonPath("$.data.nombre").value("Concierto Test Publicado"));
    }

    @Test
    void testListarEventosProximos_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testListarPorEstado_Publicado() throws Exception { // Changed state
        mockMvc.perform(get("/api/v1/eventos/estado/PUBLICADO")) // Test for PUBLICADO
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.idEvento == %d)]", eventoTestPublicado.getIdEvento()).exists()); // Check if it finds the published one
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearEvento_SinPermisoCliente() throws Exception {
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Nuevo Evento Cliente");
        dto.setFechaEvento(LocalDate.now().plusDays(10));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setAforoDisponible(100);
        dto.setIdLocal(localTest.getIdLocal());

        mockMvc.perform(post("/api/v1/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearEvento_Admin_DebeCrearComoBorrador() throws Exception { // Renamed test
        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Nuevo Evento Admin Borrador");
        dto.setFechaEvento(LocalDate.now().plusDays(30));
        dto.setTipoEvento(TipoEvento.ROCK);
        dto.setAforoDisponible(1000);
        dto.setIdLocal(localTest.getIdLocal());

        mockMvc.perform(post("/api/v1/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                // --- CORRECCIÓN ---
                .andExpect(jsonPath("$.mensaje").value("Evento creado en modo BORRADOR"))
                .andExpect(jsonPath("$.data.nombre").value("Nuevo Evento Admin Borrador"))
                .andExpect(jsonPath("$.data.estadoEvento").value("BORRADOR")) // Check state
                .andExpect(jsonPath("$.data.activo").value(false)); // Check active status
        // --- FIN CORRECCIÓN ---
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarEvento_Exitoso() throws Exception {
        // Arrange: Crear un evento borrador para actualizar
        Evento borrador = new Evento();
        borrador.setNombre("Borrador para Update");
        borrador.setFechaEvento(LocalDate.now().plusMonths(1));
        borrador.setTipoEvento(TipoEvento.ROCK);
        borrador.setEstadoEvento(EstadoEvento.BORRADOR);
        borrador.setActivo(false);
        borrador.setLocal(localTest);
        Evento eventoParaActualizar = eventoRepository.save(borrador);


        EventoCreateDTO dto = new EventoCreateDTO();
        dto.setNombre("Evento Actualizado via PUT");
        dto.setFechaEvento(LocalDate.now().plusMonths(2));
        dto.setTipoEvento(TipoEvento.POP);
        dto.setAforoDisponible(2000);
        dto.setIdLocal(localTest.getIdLocal()); // Keep or change local

        mockMvc.perform(put("/api/v1/eventos/" + eventoParaActualizar.getIdEvento())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Evento actualizado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value("Evento Actualizado via PUT"))
                .andExpect(jsonPath("$.data.estadoEvento").value("BORRADOR")); // Still BORRADOR after PUT
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarEvento_Exitoso() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/" + eventoTestPublicado.getIdEvento()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Evento desactivado exitosamente"));

        // Verify it's inactive
        mockMvc.perform(get("/api/v1/eventos/" + eventoTestPublicado.getIdEvento()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(false));
    }

    // --- NUEVOS TESTS PARA EL WIZARD ---

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testAgregarZonaAEvento_Exitoso() throws Exception {
        // Arrange: Crear evento BORRADOR
        EventoCreateDTO dtoEvento = new EventoCreateDTO();
        dtoEvento.setNombre("Evento para Zonas");
        dtoEvento.setFechaEvento(LocalDate.now().plusDays(10));
        dtoEvento.setTipoEvento(TipoEvento.ROCK);
        dtoEvento.setIdLocal(localTest.getIdLocal());

        // Perform the POST to create the draft and capture the response
        String responseString = mockMvc.perform(post("/api/v1/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEvento))
                        // --- ADD CSRF for POST if enabled in your security config ---
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                // --- END CSRF ---
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // **IMPORTANT**: You MUST parse the ID from the responseString.
        // Using a library like Jayway JsonPath is recommended.
        // For simplicity, we'll assume the ID is easily extractable or known (e.g., 1 if it's the first test)
        // Integer idEvento = JsonPath.parse(responseString).read("$.data.idEvento"); // Correct way
        Integer idEvento = eventoRepository.findAll().get(1).getIdEvento(); // Hacky way for test - find the second event created (first is in setup)

        // Arrange: DTO para Zona
        String zonaJson = "{\"nombre\":\"Platea Test\",\"aforoMax\":500}";

        // Act & Assert for adding the zone
        mockMvc.perform(post("/api/v1/eventos/" + idEvento + "/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zonaJson)
                        // --- ADD CSRF for POST ---
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                // --- END CSRF ---
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Zona agregada al evento"))
                .andExpect(jsonPath("$.data.nombre").value("Platea Test"));
    }

    // TODO: Add tests for agregarEntrada and publicarEvento in a similar way

}