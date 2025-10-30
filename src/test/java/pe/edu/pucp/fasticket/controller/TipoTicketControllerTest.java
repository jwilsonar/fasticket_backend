package pe.edu.pucp.fasticket.controller;

import java.util.UUID;

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

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.eventos.CrearTipoTicketRequestDTO;
import pe.edu.pucp.fasticket.dto.eventos.TipoTicketDTO;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepositorio;

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
    private ZonaRepositorio zonaRepositorio;

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
        // Crear una zona para el test
        Zona zona = new Zona();
        zona.setNombre("Zona Test Filtro");
        zona.setAforoMax(100);
        zona.setActivo(true);
        zona = zonaRepositorio.save(zona);

        mockMvc.perform(get("/api/v1/tipos-ticket?zona=" + zona.getIdZona()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket para zona " + zona.getIdZona() + " obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testObtenerTipoTicketPorId_Publico() throws Exception {
        // Primero crear un tipo de ticket para poder obtenerlo
        Zona zona = zonaRepositorio.findAll().stream().findFirst().orElse(null);
        if (zona == null) {
            zona = new Zona();
            zona.setNombre("Zona Test");
            zona.setAforoMax(100);
            zona.setActivo(true);
            zona = zonaRepositorio.save(zona);
        }

        String nombreUnico = generarNombreUnico("VIP Test");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP de prueba");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);

        // Crear el tipo de ticket
        String createResponse = mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extraer el ID del tipo de ticket creado
        TipoTicketDTO createdTicket = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                TipoTicketDTO.class
        );

        // Ahora obtener el tipo de ticket creado
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
        // Buscar una zona existente
        Zona zona = zonaRepositorio.findAll().stream().findFirst().orElse(null);
        if (zona == null) {
            // Si no hay zonas, crear una para el test
            zona = new Zona();
            zona.setNombre("Zona Test");
            zona.setAforoMax(100);
            zona.setActivo(true);
            zona = zonaRepositorio.save(zona);
        }

        String nombreUnico = generarNombreUnico("VIP Test");
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();
        request.setIdZona(zona.getIdZona());
        request.setNombre(nombreUnico);
        request.setDescripcion("Acceso VIP de prueba");
        request.setPrecio(150.0);
        request.setStock(50);

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket creado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value(nombreUnico))
                .andExpect(jsonPath("$.data.precio").value(150.0))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.idZona").value(zona.getIdZona()));
    }

    @Test
    public void testCrearTipoTicket_SinPermisoCliente() throws Exception {
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();
        request.setIdZona(1);
        request.setNombre("VIP Test");
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

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearTipoTicket_StockExcedeAforo() throws Exception {
        // Crear una zona específica para este test con aforo pequeño
        Zona zona = new Zona();
        zona.setNombre("Zona Test Stock Excedido");
        zona.setAforoMax(50); // Aforo pequeño para el test
        zona.setActivo(true);
        zona = zonaRepositorio.save(zona);

        String nombreUnico = generarNombreUnico("VIP Test Stock Excedido");
        CrearTipoTicketRequestDTO request = new CrearTipoTicketRequestDTO();
        request.setIdZona(zona.getIdZona());
        request.setNombre(nombreUnico);
        request.setDescripcion("Acceso VIP de prueba");
        request.setPrecio(150.0);
        request.setStock(150); // Stock mayor al aforo

        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testActualizarTipoTicket_Exitoso() throws Exception {
        // Primero crear un tipo de ticket
        Zona zona = zonaRepositorio.findAll().stream().findFirst().orElse(null);
        if (zona == null) {
            zona = new Zona();
            zona.setNombre("Zona Test");
            zona.setAforoMax(100);
            zona.setActivo(true);
            zona = zonaRepositorio.save(zona);
        }

        String nombreUnico = generarNombreUnico("VIP Test");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP de prueba");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);

        // Crear el tipo de ticket
        String createResponse = mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extraer el ID del tipo de ticket creado
        TipoTicketDTO createdTicket = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                TipoTicketDTO.class
        );

        // Actualizar el tipo de ticket
        String nombreActualizado = generarNombreUnico("VIP Actualizado Test");
        CrearTipoTicketRequestDTO updateRequest = new CrearTipoTicketRequestDTO();
        updateRequest.setIdZona(zona.getIdZona());
        updateRequest.setNombre(nombreActualizado);
        updateRequest.setDescripcion("Acceso VIP actualizado");
        updateRequest.setPrecio(200.0);
        updateRequest.setStock(75);

        mockMvc.perform(put("/api/v1/tipos-ticket/" + createdTicket.getIdTipoTicket())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket actualizado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value(nombreActualizado))
                .andExpect(jsonPath("$.data.precio").value(200.0))
                .andExpect(jsonPath("$.data.stock").value(75));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarTipoTicket_Exitoso() throws Exception {
        // Crear un tipo de ticket para eliminar
        Zona zona = zonaRepositorio.findAll().stream().findFirst().orElse(null);
        if (zona == null) {
            zona = new Zona();
            zona.setNombre("Zona Test");
            zona.setAforoMax(100);
            zona.setActivo(true);
            zona = zonaRepositorio.save(zona);
        }

        String nombreUnico = generarNombreUnico("VIP Para Eliminar");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP para eliminar");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);

        // Crear el tipo de ticket
        String createResponse = mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extraer el ID del tipo de ticket creado
        TipoTicketDTO createdTicket = objectMapper.readValue(
                objectMapper.readTree(createResponse).get("data").toString(),
                TipoTicketDTO.class
        );

        // Eliminar el tipo de ticket
        mockMvc.perform(delete("/api/v1/tipos-ticket/" + createdTicket.getIdTipoTicket()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Tipo de ticket eliminado exitosamente"));
    }

    @Test
    public void testEliminarTipoTicket_SinPermisoCliente() throws Exception {
        mockMvc.perform(delete("/api/v1/tipos-ticket/1"))
                .andExpect(status().isForbidden());
    }

    // ========== NUEVOS TESTS PARA FILTROS MEJORADOS ==========

    @Test
    public void testListarTiposTicket_FiltroPorZonaActivos() throws Exception {
        // Crear una zona para el test
        Zona zona = new Zona();
        zona.setNombre("Zona Test Activos");
        zona.setAforoMax(100);
        zona.setActivo(true);
        zona = zonaRepositorio.save(zona);

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
        // Crear una zona
        Zona zona = new Zona();
        zona.setNombre("Zona Test Con Tickets");
        zona.setAforoMax(100);
        zona.setActivo(true);
        zona = zonaRepositorio.save(zona);

        // Crear un tipo de ticket en esa zona
        String nombreUnico = generarNombreUnico("VIP Test Filtro");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP de prueba para filtro");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);

        // Crear el tipo de ticket como administrador
        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Ahora probar el filtro por zona
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
        // Crear una zona
        Zona zona = new Zona();
        zona.setNombre("Zona Test Con Tickets Activos");
        zona.setAforoMax(100);
        zona.setActivo(true);
        zona = zonaRepositorio.save(zona);

        // Crear un tipo de ticket activo en esa zona
        String nombreUnico = generarNombreUnico("VIP Test Activo");
        CrearTipoTicketRequestDTO createRequest = new CrearTipoTicketRequestDTO();
        createRequest.setIdZona(zona.getIdZona());
        createRequest.setNombre(nombreUnico);
        createRequest.setDescripcion("Acceso VIP activo de prueba");
        createRequest.setPrecio(150.0);
        createRequest.setStock(50);

        // Crear el tipo de ticket como administrador
        mockMvc.perform(post("/api/v1/tipos-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Ahora probar el filtro por zona activos
        mockMvc.perform(get("/api/v1/tipos-ticket?zona=" + zona.getIdZona() + "&activos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lista de tipos de ticket activos para zona " + zona.getIdZona() + " obtenida exitosamente"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value(nombreUnico))
                .andExpect(jsonPath("$.data[0].idZona").value(zona.getIdZona()))
                .andExpect(jsonPath("$.data[0].activo").value(true));
    }
}
