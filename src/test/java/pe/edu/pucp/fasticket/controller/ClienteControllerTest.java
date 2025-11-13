package pe.edu.pucp.fasticket.controller;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilUpdateDTO;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

/**
 * Tests de integración para ClienteController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
public class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cliente clienteTest;
    private final String emailTest = "cliente.test@fasticket.com";
    private final String passwordTest = "password123";

    @BeforeEach
    void setUp() {
        // Crear cliente de prueba
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setDocIdentidad("12345678");
        cliente.setNombres("Juan");
        cliente.setApellidos("Pérez");
        cliente.setEmail(emailTest);
        cliente.setContrasena(passwordEncoder.encode(passwordTest));
        cliente.setRol(Rol.CLIENTE);
        cliente.setActivo(true);
        cliente.setFechaCreacion(LocalDate.now());
        clienteTest = clienteRepository.save(cliente);
    }

    @Test
    @WithMockUser(username = "cliente.test@fasticket.com", roles = "CLIENTE")
    void testDesactivarMiCuenta_Exitoso() throws Exception {
        // Verificar que el cliente está activo antes
        assertTrue(clienteTest.getActivo(), "El cliente debe estar activo antes de desactivar");

        // Act & Assert
        mockMvc.perform(delete("/api/v1/clientes/mi-cuenta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.mensaje").value("Su cuenta ha sido desactivada exitosamente."));

        // Verificar que el cliente se desactivó en la base de datos
        Cliente clienteDesactivado = clienteRepository.findById(clienteTest.getIdPersona())
                .orElseThrow();
        assertFalse(clienteDesactivado.getActivo(), "El cliente debe estar desactivado después de la operación");
    }

    @Test
    void testDesactivarMiCuenta_SinAutenticacion() throws Exception {
        // Act & Assert - Sin autenticación debe retornar 403 (Forbidden)
        mockMvc.perform(delete("/api/v1/clientes/mi-cuenta"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void testDesactivarMiCuenta_ConRolIncorrecto() throws Exception {
        // Act & Assert - Un administrador no puede usar este endpoint
        mockMvc.perform(delete("/api/v1/clientes/mi-cuenta"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cliente.test@fasticket.com", roles = "CLIENTE")
    void testDesactivarMiCuenta_CuentaYaDesactivada() throws Exception {
        // Arrange - Desactivar el cliente primero
        clienteTest.setActivo(false);
        clienteRepository.save(clienteTest);

        // Act & Assert - BusinessException retorna 409 (Conflict)
        mockMvc.perform(delete("/api/v1/clientes/mi-cuenta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ya se encuentra desactivada")));
    }

    @Test
    @WithMockUser(username = "noexiste@fasticket.com", roles = "CLIENTE")
    void testDesactivarMiCuenta_ClienteNoEncontrado() throws Exception {
        // Act & Assert - Cliente con email que no existe
        mockMvc.perform(delete("/api/v1/clientes/mi-cuenta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("no encontrado")));
    }

    @Test
    @WithMockUser(username = "cliente.test@fasticket.com", roles = "CLIENTE")
    void testObtenerPerfil_Exitoso() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clientes/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.email").value(emailTest))
                .andExpect(jsonPath("$.data.nombres").value("Juan"))
                .andExpect(jsonPath("$.data.apellidos").value("Pérez"));
    }

    @Test
    void testObtenerPerfil_SinAutenticacion() throws Exception {
        // Act & Assert - Sin autenticación retorna 403 (Forbidden)
        mockMvc.perform(get("/api/v1/clientes/perfil"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cliente.test@fasticket.com", roles = "CLIENTE")
    void testActualizarPerfil_Exitoso() throws Exception {
        // Arrange
        ClientePerfilUpdateDTO dto = new ClientePerfilUpdateDTO();
        dto.setNombres("Juan Carlos");
        dto.setApellidos("Pérez García");
        dto.setTelefono("987654321");

        // Act & Assert
        mockMvc.perform(put("/api/v1/clientes/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombres").value("Juan Carlos"))
                .andExpect(jsonPath("$.data.apellidos").value("Pérez García"))
                .andExpect(jsonPath("$.data.telefono").value("987654321"));
    }

    @Test
    @WithMockUser(username = "cliente.test@fasticket.com", roles = "CLIENTE")
    void testObtenerHistorialCompras_Exitoso() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clientes/historial-compras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testObtenerHistorialCompras_SinAutenticacion() throws Exception {
        // Act & Assert - Sin autenticación retorna 403 (Forbidden)
        mockMvc.perform(get("/api/v1/clientes/historial-compras"))
                .andExpect(status().isForbidden());
    }
}

