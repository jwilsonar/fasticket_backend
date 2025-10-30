package pe.edu.pucp.fasticket.service;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.auth.CambioContrasenaDTO;
import pe.edu.pucp.fasticket.dto.auth.LoginRequestDTO;
import pe.edu.pucp.fasticket.dto.auth.LoginResponseDTO;
import pe.edu.pucp.fasticket.dto.auth.RegistroRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.auth.AuthService;

/**
 * Tests para AuthService.
 * Valida login, registro y cambio de contraseña.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PersonasRepositorio personasRepositorio;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cliente clienteExistente;
    private final String emailTest = "test@fasticket.com";
    private final String passwordTest = "password123";

    @BeforeEach
    void setUp() {
        // Crear un cliente de prueba
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
        
        clienteExistente = clienteRepository.save(cliente);
    }

    @Test
    void testLogin_Exitoso() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(emailTest, passwordTest);

        // Act
        LoginResponseDTO response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals(emailTest, response.getEmail());
        assertEquals("CLIENTE", response.getRol());
        assertEquals("Juan Pérez", response.getNombreCompleto());
    }

    @Test
    void testLogin_CredencialesInvalidas() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(emailTest, "passwordIncorrecto");

        // Act & Assert
        assertThrows(Exception.class, () -> authService.login(request));
    }

    @Test
    void testRegistro_Exitoso() {
        // Arrange
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setTipoDocumento(TipoDocumento.DNI);
        request.setDocIdentidad("87654321");
        request.setNombres("María");
        request.setApellidos("García");
        request.setEmail("maria@fasticket.com");
        request.setContrasena("password456");
        request.setTelefono("987654321");
        request.setFechaNacimiento(LocalDate.of(1995, 5, 15));

        // Act
        LoginResponseDTO response = authService.registrarCliente(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("maria@fasticket.com", response.getEmail());
        assertEquals("CLIENTE", response.getRol());

        // Verificar que se guardó en BD
        assertTrue(personasRepositorio.existsByEmail("maria@fasticket.com"));
    }

    @Test
    void testRegistro_EmailDuplicado() {
        // Arrange
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setEmail(emailTest); // Email ya existe
        request.setDocIdentidad("99999999");
        request.setContrasena("password");
        request.setNombres("Test");
        request.setApellidos("User");
        request.setTipoDocumento(TipoDocumento.DNI);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> authService.registrarCliente(request));
        assertTrue(exception.getMessage().contains("email ya está registrado"));
    }

    @Test
    void testCambiarContrasena_Exitoso() {
        // Arrange
        CambioContrasenaDTO request = new CambioContrasenaDTO();
        request.setContrasenaActual(passwordTest);
        request.setContrasenaNueva("newPassword123");
        request.setContrasenaConfirmacion("newPassword123");

        // Act
        authService.cambiarContrasena(clienteExistente.getIdPersona(), request);

        // Assert
        Cliente clienteActualizado = clienteRepository.findById(clienteExistente.getIdPersona()).get();
        assertTrue(passwordEncoder.matches("newPassword123", clienteActualizado.getContrasena()));
    }

    @Test
    void testCambiarContrasena_PasswordActualIncorrecta() {
        // Arrange
        CambioContrasenaDTO request = new CambioContrasenaDTO();
        request.setContrasenaActual("passwordIncorrecta");
        request.setContrasenaNueva("newPassword123");
        request.setContrasenaConfirmacion("newPassword123");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> authService.cambiarContrasena(clienteExistente.getIdPersona(), request));
        assertTrue(exception.getMessage().contains("contraseña actual es incorrecta"));
    }

    @Test
    void testCambiarContrasena_ConfirmacionNoCoincide() {
        // Arrange
        CambioContrasenaDTO request = new CambioContrasenaDTO();
        request.setContrasenaActual(passwordTest);
        request.setContrasenaNueva("newPassword123");
        request.setContrasenaConfirmacion("diferente123");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> authService.cambiarContrasena(clienteExistente.getIdPersona(), request));
        assertTrue(exception.getMessage().contains("no coinciden"));
    }
}

