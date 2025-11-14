package pe.edu.pucp.fasticket.controller; // OJO: Asegúrate que el package sea el correcto

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import pe.edu.pucp.fasticket.controllers.auditoria.LogController;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDetalleDTO;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogRequestDTO;
import pe.edu.pucp.fasticket.services.auditoria.LogService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

// ¡Usamos esto en lugar de las anotaciones de Spring!
@ExtendWith(MockitoExtension.class)
public class LogControllerTest {

    @Mock // <-- ¡Este es el @Mock de Mockito que SÍ te funciona!
    private LogService logService;

    @InjectMocks // <-- Esto inyecta el logService falso en el controlador
    private LogController logController;

    // NO necesitamos @Autowired ni MockMvc

    // Simula un administrador autenticado
    @Test
    public void testRegistrarErrorManual_Exitoso() throws Exception {

        // 1. Preparar Datos (Arrange)
        String adminEmail = "admin@pucp.edu.pe";

        // Creamos un mock de UserDetails (el admin logueado)
        UserDetails userDetailsMock = mock(UserDetails.class);
        when(userDetailsMock.getUsername()).thenReturn(adminEmail);

        // Creamos el DTO de solicitud
        ErrorLogRequestDTO requestDTO = new ErrorLogRequestDTO();
        requestDTO.setFechaHora(LocalDateTime.now());
        requestDTO.setSeveridad("CRITICAL");
        requestDTO.setModulo("Ventas");
        // ... (setear los demás campos si es necesario)

        // Creamos el DTO de respuesta que esperamos del servicio
        ErrorLogDetalleDTO respuestaDTO = new ErrorLogDetalleDTO();
        respuestaDTO.setIdError(1);
        respuestaDTO.setNombreAdmin("Admin de Prueba");

        // 2. Definir Comportamiento de Mocks (Arrange)
        // Cuando el controlador llame a logService.registrarErrorManual...
        when(logService.registrarErrorManual(any(ErrorLogRequestDTO.class), eq(adminEmail)))
                .thenReturn(respuestaDTO);

        // 3. Ejecutar el método (Act)
        // Llamamos al método del controlador DIRECTAMENTE
        ResponseEntity<StandardResponse<ErrorLogDetalleDTO>> responseEntity =
                logController.registrarErrorManual(requestDTO, userDetailsMock);

        // 4. Verificar Resultados (Assert)
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().getOk());
        assertEquals("Error registrado exitosamente", responseEntity.getBody().getMensaje());
        assertEquals(1, responseEntity.getBody().getData().getIdError());
        assertEquals("Admin de Prueba", responseEntity.getBody().getData().getNombreAdmin());
    }
}