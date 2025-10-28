package pe.edu.pucp.fasticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import pe.edu.pucp.fasticket.model.eventos.*;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para EventoController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventosRepositorio eventoRepository;

    @Autowired // Necesitarás estos repositorios
    private LocalesRepositorio localRepositorio;
    @Autowired
    private TipoTicketRepositorio tipoTicketRepositorio;

    private Evento eventoTest;
    private Local localTest;
    private TipoTicket tipoTicketTest;

    @BeforeEach
    void setUp() {
        localTest = new Local();
        localTest.setNombre("Estadio Prueba Setup");
        localTest.setDireccion("Dirección Test 456");
        localTest.setAforoTotal(5000);
        localTest.setActivo(true);
        localTest = localRepositorio.save(localTest);
        Evento evento = new Evento();
        evento.setNombre("Concierto Test Setup");
        evento.setDescripcion("Descripción test setup");
        evento.setFechaEvento(LocalDate.now().plusMonths(1));
        evento.setHoraInicio(LocalTime.of(20, 0));
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setEstadoEvento(EstadoEvento.ACTIVO);
        evento.setAforoDisponible(5000);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());
        evento.setLocal(localTest);
        eventoTest = eventoRepository.save(evento);
        tipoTicketTest = new TipoTicket();
        tipoTicketTest.setNombre("General Setup");
        tipoTicketTest.setPrecio(120.0);
        tipoTicketTest.setStock(1000);
        tipoTicketTest.setCantidadDisponible(1000);
        tipoTicketTest.setEvento(eventoTest);
        tipoTicketTest.setActivo(true);
        tipoTicketTest = tipoTicketRepositorio.save(tipoTicketTest);
    }

    @AfterEach
    void tearDown() {
        tipoTicketRepositorio.deleteAll();
        eventoRepository.deleteAll();
        localRepositorio.deleteAll();
    }

    @Test
    void testListarEventos_SinAutenticacion() throws Exception {
        // Este endpoint es público
        mockMvc.perform(get("/api/v1/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testObtenerEventoPorId_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/" + eventoTest.getIdEvento()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idEvento").value(eventoTest.getIdEvento()))
                .andExpect(jsonPath("$.data.nombre").value("Concierto Test Setup"));
    }

    @Test
    void testListarEventosProximos_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testListarPorEstado_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/estado/ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
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
        String eventoJson = String.format(
                "{\"nombre\":\"Nuevo Evento Admin\",\"fechaEvento\":\"2025-12-31\",\"tipoEvento\":\"ROCK\",\"aforoDisponible\":1000,\"idLocal\":%d}",
                localTest.getIdLocal()
        );

        mockMvc.perform(post("/api/v1/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Nuevo Evento Admin"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testActualizarEvento_Exitoso() throws Exception {
        String eventoJson = "{\"nombre\":\"Evento Actualizado\",\"fechaEvento\":\"2025-12-31\",\"tipoEvento\":\"POP\",\"aforoDisponible\":2000}";

        mockMvc.perform(put("/api/v1/eventos/" + eventoTest.getIdEvento())
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Evento Actualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarEvento_Exitoso() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/" + eventoTest.getIdEvento()))
                .andExpect(status().isOk());
    }

    @Test
    void testObtenerDetalleParaCompra_Publico() throws Exception {
        // Asegúrate que eventoTest tiene un Local y Tipos de Ticket asociados en el setUp()

        mockMvc.perform(get("/api/v1/eventos/" + eventoTest.getIdEvento() + "/detalle-compra"))
                .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true)) // Cambiado de 'ok' a 'success'
                .andExpect(jsonPath("$.mensajeAviso").value("Detalle de evento obtenido exitosamente")) // Cambiado de 'mensaje' a 'mensajeAviso'
                .andExpect(jsonPath("$.data.id").value(eventoTest.getIdEvento())) // Verifica campos del EventoDetalleDTO
                .andExpect(jsonPath("$.data.nombre").value(eventoTest.getNombre()))
                .andExpect(jsonPath("$.data.local.nombre").exists()) // Verifica que el local está
                .andExpect(jsonPath("$.data.tiposDeTicket").isArray()) // Verifica que hay lista de tickets
                .andExpect(jsonPath("$.data.tiposDeTicket", hasSize(greaterThan(0)))); // Verifica que no está vacía
    }

    @Test
    void testObtenerDetalleParaCompra_NoEncontrado() throws Exception {
        mockMvc.perform(get("/api/v1/eventos/9999/detalle-compra")) // ID que no existe
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false)) // Asegúrate que tu GlobalExceptionHandler devuelva esto
                .andExpect(jsonPath("$.message").value(containsString("Evento no encontrado"))); // Asegúrate que tu GlobalExceptionHandler devuelva esto
    }
}

