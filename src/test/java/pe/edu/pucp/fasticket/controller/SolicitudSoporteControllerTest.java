package pe.edu.pucp.fasticket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.pucp.fasticket.controllers.soporte.SolicitudSoporteController;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarEstadoSolicitudDTO;
import pe.edu.pucp.fasticket.dto.soporte.ActualizarSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.CrearSolicitudSoporteDTO;
import pe.edu.pucp.fasticket.dto.soporte.SolicitudSoporteResponseDTO;
import pe.edu.pucp.fasticket.model.soporte.EstadoSoporte;
import pe.edu.pucp.fasticket.model.soporte.PrioridadSoporte;
import pe.edu.pucp.fasticket.services.soporte.SolicitudSoporteService;
import pe.edu.pucp.fasticket.services.auditoria.LogService;
import pe.edu.pucp.fasticket.security.JwtUtil;

@WebMvcTest(SolicitudSoporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SolicitudSoporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SolicitudSoporteService soporteService;

    @MockBean
    private LogService logService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    private SolicitudSoporteResponseDTO baseResponse;

    @BeforeEach
    void setUp() {
        baseResponse = SolicitudSoporteResponseDTO.builder()
                .idSolicitud(1L)
                .idUsuario(10)
                .nombreUsuario("Juan Pérez")
                .emailUsuario("juan@test.com")
                .asunto("Problema con login")
                .mensaje("No puedo iniciar sesión")
                .estado(EstadoSoporte.ABIERTO)
                .prioridad(PrioridadSoporte.ALTA)
                .canalOrigen("PORTAL_WEB")
                .ipOrigen("127.0.0.1")
                .build();
    }

    @Test
    void crearSolicitud_debeRetornarCreated() throws Exception {
        CrearSolicitudSoporteDTO request = new CrearSolicitudSoporteDTO();
        request.setAsunto("Problema con login");
        request.setMensaje("No puedo iniciar sesión");

        when(soporteService.crear(any(CrearSolicitudSoporteDTO.class), any())).thenReturn(baseResponse);

        mockMvc.perform(post("/api/v1/soporte")
                .with(user("test@fasticket.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.idSolicitud").value(1L))
                .andExpect(jsonPath("$.data.asunto").value("Problema con login"));

        verify(soporteService).crear(any(CrearSolicitudSoporteDTO.class), any());
    }

    @Test
    void listarSolicitudes_debeRetornarOkConLista() throws Exception {
        when(soporteService.listar(null, null)).thenReturn(List.of(baseResponse));

        mockMvc.perform(get("/api/v1/soporte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].idSolicitud").value(1L));

        verify(soporteService).listar(null, null);
    }

    @Test
    void obtenerSolicitudPorId_debeRetornarOk() throws Exception {
        when(soporteService.obtenerPorId(1L)).thenReturn(baseResponse);

        mockMvc.perform(get("/api/v1/soporte/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ABIERTO"));

        verify(soporteService).obtenerPorId(1L);
    }

    @Test
    void actualizarSolicitud_debeRetornarOk() throws Exception {
        ActualizarSolicitudSoporteDTO dto = new ActualizarSolicitudSoporteDTO();
        dto.setAsunto("Nuevo asunto");

        SolicitudSoporteResponseDTO updated = baseResponse.toBuilder()
                .asunto("Nuevo asunto")
                .build();

        when(soporteService.actualizar(eq(1L), any(ActualizarSolicitudSoporteDTO.class), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/soporte/1")
                .with(user("test@fasticket.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asunto").value("Nuevo asunto"));

        verify(soporteService).actualizar(eq(1L), any(ActualizarSolicitudSoporteDTO.class), any());
    }

    @Test
    void actualizarEstado_debeRetornarOkYEstadoActualizado() throws Exception {
        ActualizarEstadoSolicitudDTO estadoDTO = new ActualizarEstadoSolicitudDTO();
        estadoDTO.setEstado(EstadoSoporte.EN_PROGRESO);

        SolicitudSoporteResponseDTO updated = baseResponse.toBuilder()
                .estado(EstadoSoporte.EN_PROGRESO)
                .build();

        when(soporteService.actualizarEstado(eq(1L), any(ActualizarEstadoSolicitudDTO.class), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/soporte/1/estado")
                .with(user("test@fasticket.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(estadoDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("EN_PROGRESO"));

        verify(soporteService).actualizarEstado(eq(1L), any(ActualizarEstadoSolicitudDTO.class), any());
    }

    @Test
    void eliminarSolicitud_debeRetornarOk() throws Exception {
        mockMvc.perform(delete("/api/v1/soporte/1")
                .with(user("test@fasticket.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Ticket eliminado lógicamente"));

        verify(soporteService, times(1)).eliminar(eq(1L), any());
    }
}

