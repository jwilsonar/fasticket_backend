package pe.edu.pucp.fasticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.fasticket.dto.auth.LoginRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.RegistroRequestDTO;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para AuthController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String emailTest = "test@fasticket.com";
    private String passwordTest = "password123";

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
        clienteRepository.save(cliente);
    }

    @Test
    void testLogin_Exitoso() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(emailTest, passwordTest);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value(emailTest))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void testLogin_CredencialesInvalidas() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(emailTest, "passwordIncorrecta");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegistro_Exitoso() throws Exception {
        // Arrange
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setTipoDocumento(TipoDocumento.DNI);
        request.setDocIdentidad("87654321");
        request.setNombres("María");
        request.setApellidos("García");
        request.setEmail("maria@fasticket.com");
        request.setContrasena("password456");
        request.setTelefono("987654321");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("maria@fasticket.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void testRegistro_EmailDuplicado() throws Exception {
        // Arrange - Usar email que ya existe
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setTipoDocumento(TipoDocumento.DNI);
        request.setDocIdentidad("99999999");
        request.setNombres("Test");
        request.setApellidos("User");
        request.setEmail(emailTest); // Email duplicado
        request.setContrasena("password");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testRegistro_DatosInvalidos() throws Exception {
        // Arrange - Request sin campos obligatorios
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setEmail("invalido"); // Email inválido
        // Falta nombre, apellido, etc.

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

