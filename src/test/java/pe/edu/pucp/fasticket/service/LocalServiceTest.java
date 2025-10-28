package pe.edu.pucp.fasticket.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.fasticket.config.TestConfig;
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalResponseDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.geografia.Departamento;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.geografia.Provincia;
import pe.edu.pucp.fasticket.repository.geografia.DepartamentoRepository;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.geografia.ProvinciaRepository;
import pe.edu.pucp.fasticket.services.eventos.LocalService;

/**
 * Tests para LocalService.
 * Valida operaciones CRUD de locales.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
public class LocalServiceTest {

    @Autowired
    private LocalService localService;

    @Autowired
    private DistritoRepository distritoRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    private Distrito distritoTest;

    @BeforeEach
    void setUp() {
        // Crear estructura geográfica de prueba
        Departamento departamento = new Departamento();
        departamento.setNombre("Lima");
        departamento.setActivo(true);
        departamento = departamentoRepository.save(departamento);

        Provincia provincia = new Provincia();
        provincia.setNombre("Lima");
        provincia.setDepartamento(departamento);
        provincia.setActivo(true);
        provincia = provinciaRepository.save(provincia);

        Distrito distrito = new Distrito();
        distrito.setNombre("Lima Cercado");
        distrito.setProvincia(provincia);
        distrito.setActivo(true);
        distritoTest = distritoRepository.save(distrito);
    }

    @Test
    void testCrearLocal_Exitoso() {
        // Arrange
        LocalCreateDTO dto = new LocalCreateDTO();
        dto.setNombre("Estadio Nacional");
        dto.setDireccion("Av. José Díaz");
        dto.setAforoTotal(45000);
        dto.setIdDistrito(distritoTest.getIdDistrito());

        // Act
        LocalResponseDTO response = localService.crear(dto);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getIdLocal());
        assertEquals("Estadio Nacional", response.getNombre());
        assertEquals(45000, response.getAforoTotal());
        assertTrue(response.getActivo());
    }

    @Test
    void testCrearLocal_NombreDuplicado() {
        // Arrange - Crear primer local
        LocalCreateDTO dto1 = new LocalCreateDTO();
        dto1.setNombre("Estadio Test");
        dto1.setAforoTotal(10000);
        localService.crear(dto1);

        // Intentar crear otro con el mismo nombre
        LocalCreateDTO dto2 = new LocalCreateDTO();
        dto2.setNombre("Estadio Test");
        dto2.setAforoTotal(15000);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> localService.crear(dto2));
        assertTrue(exception.getMessage().contains("Ya existe un local"));
    }

    @Test
    void testListarLocales() {
        // Arrange - Crear varios locales
        LocalCreateDTO dto1 = new LocalCreateDTO();
        dto1.setNombre("Local 1");
        dto1.setAforoTotal(1000);
        localService.crear(dto1);

        LocalCreateDTO dto2 = new LocalCreateDTO();
        dto2.setNombre("Local 2");
        dto2.setAforoTotal(2000);
        localService.crear(dto2);

        // Act
        List<LocalResponseDTO> locales = localService.listarTodos();

        // Assert
        assertTrue(locales.size() >= 2);
    }

    @Test
    void testActualizarLocal_Exitoso() {
        // Arrange - Crear local
        LocalCreateDTO dtoCrear = new LocalCreateDTO();
        dtoCrear.setNombre("Local Original");
        dtoCrear.setAforoTotal(5000);
        LocalResponseDTO localCreado = localService.crear(dtoCrear);

        // Preparar actualización
        LocalCreateDTO dtoActualizar = new LocalCreateDTO();
        dtoActualizar.setNombre("Local Actualizado");
        dtoActualizar.setAforoTotal(7000);

        // Act
        LocalResponseDTO localActualizado = localService.actualizar(localCreado.getIdLocal(), dtoActualizar);

        // Assert
        assertEquals("Local Actualizado", localActualizado.getNombre());
        assertEquals(7000, localActualizado.getAforoTotal());
    }

    @Test
    void testObtenerLocalPorId_NoExiste() {
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> localService.obtenerPorId(99999));
        assertTrue(exception.getMessage().contains("Local no encontrado"));
    }

    @Test
    void testEliminarLocal_Logico() {
        // Arrange - Crear local
        LocalCreateDTO dto = new LocalCreateDTO();
        dto.setNombre("Local a Eliminar");
        dto.setAforoTotal(3000);
        LocalResponseDTO local = localService.crear(dto);

        // Act
        localService.eliminarLogico(local.getIdLocal());

        // Assert - El local debe estar inactivo
        LocalResponseDTO localConsultado = localService.obtenerPorId(local.getIdLocal());
        assertFalse(localConsultado.getActivo());
    }
}

