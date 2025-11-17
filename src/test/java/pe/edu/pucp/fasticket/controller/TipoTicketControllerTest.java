package pe.edu.pucp.fasticket.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.DateTime;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.eventos.ActualizarTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.CrearTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepository;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestConfig.class)
public class TipoTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZonaRepository zonaRepositorio;

    @Autowired
    private LocalesRepositorio localesRepositorio;

    @Autowired
    private EventosRepositorio eventosRepositorio;

    @Autowired
    private TipoTicketRepositorio tipoTicketRepositorio;

    private Evento eventoTest;
    private Zona zonaTest;

    private Zona crearZonaDePrueba() {
        Local local = new Local();
        local.setNombre("Local para Test");
        local.setAforoTotal(100);
        local.setActivo(true);
        localesRepositorio.save(local);

        Evento evento = new Evento();
        evento.setNombre("Evento para Test");
        evento.setFechaEvento(LocalDate.now().plusDays(1));
        evento.setHoraInicio(LocalTime.now());
        evento.setEstadoEvento(EstadoEvento.PUBLICADO);
        evento.setActivo(true);
        evento.setLocal(local);
        eventoTest = eventosRepositorio.save(evento);

        Zona zona = new Zona();
        zona.setNombre("Zona Test");
        zona.setAforoMax(100);
        zona.setActivo(true);
        zona.setEvento(eventoTest);
        zonaTest = zonaRepositorio.save(zona);
        return zonaTest;
    }

    private String generarNombreUnico(String prefijo) {
        return prefijo + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    public void testListarTiposTicket_Publico() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testListarTiposTicket_FiltroPorZona() throws Exception {
        Zona zona = crearZonaDePrueba();

        mockMvc.perform(get("/api/v1/tipos-ticket?zona=" + zona.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket para zona " + zona.getIdZona() + " obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testObtenerTipoTicketPorId_Publico() throws Exception {
        Zona zona = crearZonaDePrueba();
        String nombreUnico = generarNombreUnico("VIP Test");

        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP de prueba");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);
        // Recomendación: usar java.time.LocalDate para almacenar solo la fecha (sin hora).
        // Ejemplo si tu DTO/entidad usa LocalDate:
        LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
        LocalDate fechaFin = LocalDate.of(2024, 7, 31);
        createRequest.setFechaInicioVenta(fechaInicio);
        createRequest.setFechaFinVenta(fechaFin);

        String createResponse = mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TipoTicketDTO createdTicket = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                TipoTicketDTO.class
        );

        mockMvc.perform(get("/api/v1/tipos-ticket/" + createdTicket.getIdTipoTicket()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket obtenido exitosamente"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testObtenerTipoTicketPorId_NoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearTipoTicket_ConPermisoAdmin() throws Exception {
        Zona zona = crearZonaDePrueba();

        String nombreUnico = generarNombreUnico("VIP Test");
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();

        LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
        LocalDate fechaFin = LocalDate.of(2024, 7, 31);

        request.setIdZona(zona.getIdZona());
        request.setNombre(nombreUnico);
        request.setDescripcion("Acceso VIP de prueba");
        request.setPrecio(150.0);
        request.setStock(50);
        request.setFechaInicioVenta(fechaInicio);
        request.setFechaFinVenta(fechaFin);

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket creado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value(nombreUnico))
                .andExpect(jsonPath("$.data.precio").value(150.0))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.fechaInicioVenta").value("2024-07-01"))
                .andExpect(jsonPath("$.data.fechaFinVenta").value("2024-07-31"))
                .andExpect(jsonPath("$.data.idZona").value(zona.getIdZona()));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    public void testCrearTipoTicket_SinPermisoCliente() throws Exception {
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();
        request.setIdZona(1);
        request.setNombre("VIP Test");
        LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
        LocalDate fechaFin = LocalDate.of(2024, 7, 31);
        request.setFechaInicioVenta(fechaInicio);
        request.setFechaFinVenta(fechaFin);
        request.setDescripcion("Acceso VIP de prueba");
        request.setPrecio(150.0);
        request.setStock(50);

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearTipoTicket_ZonaNoExiste() throws Exception {
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();
        request.setIdZona(999);
        request.setNombre("VIP Test");
        request.setDescripcion("Acceso VIP de prueba");
        request.setPrecio(150.0);
        request.setStock(50);
        LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
        LocalDate fechaFin = LocalDate.of(2024, 7, 31);
        request.setFechaInicioVenta(fechaInicio);
        request.setFechaFinVenta(fechaFin);

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearTipoTicket_StockExcedeAforo() throws Exception {
        Zona zona = crearZonaDePrueba();

        String nombreUnico = generarNombreUnico("VIP Test Stock Excedido");
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();
        request.setIdZona(zona.getIdZona());
        request.setNombre(nombreUnico);
        request.setDescripcion("Acceso VIP de prueba");
        request.setPrecio(150.0);
        LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
        LocalDate fechaFin = LocalDate.of(2024, 7, 31);
        request.setFechaInicioVenta(fechaInicio);
        request.setFechaFinVenta(fechaFin);
        request.setStock(150);

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testActualizarTipoTicket_Exitoso() throws Exception {
        Zona zona = crearZonaDePrueba();

        String nombreUnico = generarNombreUnico("VIP Test");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP de prueba");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);
        createRequest.setFechaInicioVenta(LocalDate.of(2024, 7, 1));
        createRequest.setFechaFinVenta(LocalDate.of(2024, 7, 31));

        String createResponse = mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TipoTicketDTO createdTicket = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                TipoTicketDTO.class
        );

        String nombreActualizado = generarNombreUnico("VIP Actualizado Test");
        ActualizarTipoTicketRequestDTO updateRequest = new ActualizarTipoTicketRequestDTO();
        updateRequest.setNombre(nombreActualizado);
        updateRequest.setDescripcion("Acceso VIP actualizado");
        updateRequest.setPrecio(200.0);
        updateRequest.setStock(75);
        updateRequest.setFechaInicioVenta(LocalDate.of(2024, 8, 1));
        updateRequest.setFechaFinVenta(LocalDate.of(2024, 8, 31));

        mockMvc.perform(put("/api/v1/tipos-ticket/" + createdTicket.getIdTipoTicket())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket actualizado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value(nombreActualizado))
                .andExpect(jsonPath("$.data.precio").value(200.0))
                .andExpect(jsonPath("$.data.stock").value(75))
                .andExpect(jsonPath("$.data.fechaInicioVenta").value("2024-08-01"))
                .andExpect(jsonPath("$.data.fechaFinVenta").value("2024-08-31"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarTipoTicket_Exitoso() throws Exception {
        Zona zona = crearZonaDePrueba();

        String nombreUnico = generarNombreUnico("VIP Para Eliminar");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP para eliminar");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);
        createRequest.setFechaInicioVenta(LocalDate.of(2024, 7, 1));
        createRequest.setFechaFinVenta(LocalDate.of(2024, 7, 31));

        String createResponse = mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TipoTicketDTO createdTicket = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                TipoTicketDTO.class
        );

        mockMvc.perform(delete("/api/v1/tipos-ticket/" + createdTicket.getIdTipoTicket()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket eliminado exitosamente"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    public void testEliminarTipoTicket_SinPermisoCliente() throws Exception {
        mockMvc.perform(delete("/api/v1/tipos-ticket/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testListarTiposTicket_FiltroPorZonaActivos() throws Exception {
        Zona zona = crearZonaDePrueba();

        mockMvc.perform(get("/api/v1/tipos-ticket?zona=" + zona.getIdZona() + "&activos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket activos para zona " + zona.getIdZona() + " obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testListarTiposTicket_FiltroSoloActivos() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket?activos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testListarTiposTicket_ZonaNoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket?zona=999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("Zona no encontrada con ID: 999"));
    }

    @Test
    public void testListarTiposTicket_ZonaNoExisteConActivos() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket?zona=999&activos=true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("Zona no encontrada con ID: 999"));
    }

    @Test
    public void testListarTiposTicket_IdZonaInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket?zona=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("El ID de zona debe ser un número positivo"));
    }

    @Test
    public void testListarTiposTicket_IdZonaNegativo() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-ticket?zona=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("El ID de zona debe ser un número positivo"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testListarTiposTicket_ConTiposTicketEnZona() throws Exception {
        Zona zona = crearZonaDePrueba();

        String nombreUnico = generarNombreUnico("VIP Test Filtro");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP de prueba para filtro");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);
        createRequest.setFechaInicioVenta(LocalDate.of(2024, 7, 1));
        createRequest.setFechaFinVenta(LocalDate.of(2024, 7, 31 ));

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tipos-ticket?zona=" + zona.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket para zona " + zona.getIdZona() + " obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value(nombreUnico))
                .andExpect(jsonPath("$.data[0].idZona").value(zona.getIdZona()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testListarTiposTicket_ConTiposTicketActivosEnZona() throws Exception {
        Zona zona = crearZonaDePrueba();

        String nombreUnico = generarNombreUnico("VIP Test Activo");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP activo de prueba");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);
        createRequest.setFechaInicioVenta(LocalDate.of(2024, 7, 1));
        createRequest.setFechaFinVenta(LocalDate.of(2024, 7, 31));

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tipos-ticket?zona=" + zona.getIdZona() + "&activos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket activos para zona " + zona.getIdZona() + " obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value(nombreUnico))
                .andExpect(jsonPath("$.data[0].idZona").value(zona.getIdZona()))
                .andExpect(jsonPath("$.data[0].fechaInicioVenta").value("2024-07-01"))
                .andExpect(jsonPath("$.data[0].fechaFinVenta").value("2024-07-31"));
    }
}