package pe.edu.pucp.fasticket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogDetalleDTO;
import pe.edu.pucp.fasticket.dto.auditoria.ErrorLogRequestDTO;
import pe.edu.pucp.fasticket.mapper.ErrorLogMapper;
import pe.edu.pucp.fasticket.model.auditoria.ErrorLog;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.repository.auditoria.ErrorLogRepository;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.services.auditoria.LogService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LogServiceTests {

    @Mock
    private ErrorLogRepository errorLogRepository;
    @Mock
    private AdministradorRepository administradorRepository;
    @Mock
    private ErrorLogMapper errorLogMapper;

    @InjectMocks // <-- Esto inyecta los @Mock de arriba en el servicio
    private LogService logService;

    @Test
    public void testRegistrarErrorManual_Exitoso() {
        // 1. Preparar Datos (Arrange)
        String adminEmail = "admin@fasticket.com";
        ErrorLogRequestDTO requestDTO = new ErrorLogRequestDTO();
        requestDTO.setModulo("Pagos");
        requestDTO.setFechaHora(LocalDateTime.now());
        // ... (setear los demás campos del requestDTO)

        Administrador adminMock = new Administrador();
        adminMock.setIdPersona(1);
        adminMock.setNombres("Admin");
        adminMock.setApellidos("Prueba");
        adminMock.setEmail(adminEmail);

        ErrorLog errorLogGuardadoMock = new ErrorLog();
        errorLogGuardadoMock.setIdError(1);
        errorLogGuardadoMock.setModulo("Pagos");
        errorLogGuardadoMock.setAdministrador(adminMock);

        ErrorLogDetalleDTO detalleDTOMock = new ErrorLogDetalleDTO();
        detalleDTOMock.setIdError(1);
        detalleDTOMock.setModulo("Pagos");
        detalleDTOMock.setNombreAdmin("Admin Prueba");

        // 2. Definir Comportamiento de Mocks (Arrange)
        when(administradorRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminMock));
        when(errorLogRepository.save(any(ErrorLog.class))).thenReturn(errorLogGuardadoMock);
        when(errorLogMapper.toDetalleDTO(errorLogGuardadoMock)).thenReturn(detalleDTOMock);

        // 3. Ejecutar el método a probar (Act)
        ErrorLogDetalleDTO resultado = logService.registrarErrorManual(requestDTO, adminEmail);

        // 4. Verificar Resultados (Assert)
        assertNotNull(resultado);
        assertEquals(1, resultado.getIdError());
        assertEquals("Pagos", resultado.getModulo());
        assertEquals("Admin Prueba", resultado.getNombreAdmin());

        // Verificar que los mocks se llamaron
        verify(administradorRepository, times(1)).findByEmail(adminEmail);
        verify(errorLogRepository, times(1)).save(any(ErrorLog.class));
        verify(errorLogMapper, times(1)).toDetalleDTO(errorLogGuardadoMock);
    }
}