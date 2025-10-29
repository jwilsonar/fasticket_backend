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

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepositorio;

/**
 * Tests de integración para EventoController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
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
    
    @Autowired
    private ZonaRepositorio zonaRepositorio;

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
        // Crear zona de prueba
        Zona zonaTest = new Zona();
        zonaTest.setNombre("Zona Test");
        zonaTest.setAforoMax(1000);
        zonaTest.setActivo(true);
        zonaTest.setLocal(localTest);
        zonaTest = zonaRepositorio.save(zonaTest);
        
        tipoTicketTest.setZona(zonaTest);
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
    void testCrearEvento_ConImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "test.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/eventos/con-imagen")
                        .file(imagen)
                        .param("nombre", "Evento Con Imagen")
                        .param("descripcion", "Descripción del evento con imagen")
                        .param("fechaEvento", "2025-12-31")
                        .param("horaInicio", "20:00")
                        .param("horaFin", "23:00")
                        .param("tipoEvento", "ROCK")
                        .param("estadoEvento", "ACTIVO")
                        .param("aforoDisponible", "1000")
                        .param("idLocal", localTest.getIdLocal().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Evento Con Imagen"))
                .andExpect(jsonPath("$.data.descripcion").value("Descripción del evento con imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearEvento_SoloImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "test.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/eventos/con-imagen")
                        .file(imagen))
                .andExpect(status().isBadRequest()) // Debería fallar sin datos del evento
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("Se requiere información del evento"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearEvento_ConImagen_TodosLosCampos() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen completa de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "complete.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/eventos/con-imagen")
                        .file(imagen)
                        .param("nombre", "Evento Completo Con Imagen")
                        .param("descripcion", "Descripción completa del evento con todos los campos")
                        .param("fechaEvento", "2026-06-15")
                        .param("horaInicio", "18:30")
                        .param("horaFin", "23:45")
                        .param("tipoEvento", "ELECTRONICA")
                        .param("estadoEvento", "ACTIVO")
                        .param("aforoDisponible", "3000")
                        .param("idLocal", localTest.getIdLocal().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Evento Completo Con Imagen"))
                .andExpect(jsonPath("$.data.descripcion").value("Descripción completa del evento con todos los campos"))
                .andExpect(jsonPath("$.data.tipoEvento").value("ELECTRONICA"))
                .andExpect(jsonPath("$.data.estadoEvento").value("ACTIVO"))
                .andExpect(jsonPath("$.data.aforoDisponible").value(3000))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
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
    void testActualizarEvento_ConImagen() throws Exception {
        // Crear un archivo de prueba
        byte[] imagenBytes = "imagen actualizada de prueba".getBytes();
        org.springframework.mock.web.MockMultipartFile imagen = 
            new org.springframework.mock.web.MockMultipartFile("imagen", "updated.jpg", "image/jpeg", imagenBytes);

        mockMvc.perform(multipart("/api/v1/eventos/" + eventoTest.getIdEvento() + "/con-imagen")
                        .file(imagen)
                        .param("nombre", "Evento Actualizado Con Imagen")
                        .param("descripcion", "Descripción actualizada del evento con imagen")
                        .param("fechaEvento", "2025-12-31")
                        .param("horaInicio", "19:00")
                        .param("horaFin", "22:30")
                        .param("tipoEvento", "POP")
                        .param("estadoEvento", "ACTIVO")
                        .param("aforoDisponible", "2000")
                        .param("idLocal", localTest.getIdLocal().toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Evento Actualizado Con Imagen"))
                .andExpect(jsonPath("$.data.descripcion").value("Descripción actualizada del evento con imagen"))
                .andExpect(jsonPath("$.data.imagenUrl").exists());
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

