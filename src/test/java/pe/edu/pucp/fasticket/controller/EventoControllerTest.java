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
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para EventoController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventosRepositorio eventoRepository;

    private Evento eventoTest;

    @BeforeEach
    void setUp() {
        // Crear evento de prueba
        Evento evento = new Evento();
        evento.setNombre("Concierto Test");
        evento.setDescripcion("Descripción test");
        evento.setFechaEvento(LocalDate.now().plusMonths(1));
        evento.setHoraInicio(LocalTime.of(20, 0));
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setEstadoEvento(EstadoEvento.ACTIVO);
        evento.setAforoDisponible(5000);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());
        eventoTest = eventoRepository.save(evento);
    }

    @Test
    void testListarEventos_SinAutenticacion() throws Exception {
        // Este endpoint es público
        mockMvc.perform(get("/api/v1/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testObtenerEventoPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/" + eventoTest.getIdEvento()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(eventoTest.getIdEvento()))
                .andExpect(jsonPath("$.nombre").value("Concierto Test"));
    }

    @Test
    void testListarEventosProximos_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testListarPorEstado_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/estado/ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void testCrearEvento_SinPermisoCliente() throws Exception {
        // Los clientes no pueden crear eventos
        String eventoJson = "{\"nombre\":\"Nuevo Evento\",\"fechaEvento\":\"2025-12-31\",\"tipoEvento\":\"ROCK\",\"aforoDisponible\":1000}";

        mockMvc.perform(post("/api/v1/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventoJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearEvento_ConPermisoAdmin() throws Exception {
        // Los administradores sí pueden crear eventos
        String eventoJson = "{\"nombre\":\"Nuevo Evento Admin\",\"fechaEvento\":\"2025-12-31\",\"tipoEvento\":\"ROCK\",\"aforoDisponible\":1000}";

        mockMvc.perform(post("/api/v1/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nuevo Evento Admin"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarEvento_Exitoso() throws Exception {
        String eventoJson = "{\"nombre\":\"Evento Actualizado\",\"fechaEvento\":\"2025-12-31\",\"tipoEvento\":\"POP\",\"aforoDisponible\":2000}";

        mockMvc.perform(put("/api/v1/eventos/" + eventoTest.getIdEvento())
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Evento Actualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarEvento_Exitoso() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/" + eventoTest.getIdEvento()))
                .andExpect(status().isNoContent());
    }
}

