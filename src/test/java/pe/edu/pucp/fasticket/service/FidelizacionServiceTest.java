package pe.edu.pucp.fasticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.fasticket.config.TestConfig;

import pe.edu.pucp.fasticket.dto.fidelizacion.CodigoPromocionalRequestDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.ReglaPuntosRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal; // IMPORTANTE
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.EstadoEvento;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Local;
import pe.edu.pucp.fasticket.model.eventos.TipoEvento;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.fidelizacion.CodigoPromocional;
import pe.edu.pucp.fasticket.model.fidelizacion.Puntos;
import pe.edu.pucp.fasticket.model.fidelizacion.ReglaPuntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoCodigoPromocional;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoRegla;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoTransaccion;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository; // IMPORTANTE
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.*;
import pe.edu.pucp.fasticket.repository.fidelizacion.CanjeRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.CodigoPromocionalRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.PuntosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.ReglaPuntosRepository;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
@DisplayName("Tests para FidelizacionService")
class FidelizacionServiceTest {

    @Autowired
    private FidelizacionService fidelizacionService;

    @Autowired private ReglaPuntosRepository reglaPuntosRepository;
    @Autowired private PuntosRepository puntosRepository;
    @Autowired private CodigoPromocionalRepository codigoPromocionalRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private OrdenCompraRepositorio ordenCompraRepositorio;
    @Autowired private EventosRepositorio eventosRepositorio;
    @Autowired private LocalesRepositorio localesRepositorio;
    @Autowired private TipoTicketRepository tipoTicketRepository;
    @Autowired private ZonaRepository zonaRepositorio;
    @Autowired private AdministradorRepository administradorRepository;
    @Autowired private CanjeRepository canjeRepository;
    @Autowired private OrdenCompraRepositorio ordenCompraRepository;
    // INYECCIÓN NUEVA
    @Autowired private ConfiguracionRepository configuracionRepository;

    private Cliente clientePrueba;
    private ReglaPuntos reglaCompra;
    private ReglaPuntos reglaCanje;
    private Administrador adminPrueba;
    @Autowired
    private EntityManager entityManager;
    @BeforeEach
    void setUp() {
        // 1. Configurar Entorno Global (CRÍTICO PARA QUE LOS TESTS PASEN)
        configuracionRepository.deleteAll(); // Limpiar previos

        // Configuración de Puntos (1 Sol = 1 Punto, según tu última lógica)
        guardarConfig("PUNTOS_POR_MONEDA", "1");
        guardarConfig("PUNTOS_PARA_DESCONTAR_UN_SOL", "10");

        // Configuración de Niveles
        guardarConfig("NIVEL_SILVER_MIN_PUNTOS", "1000");
        guardarConfig("NIVEL_GOLD_MIN_PUNTOS", "5000");

        // Configuración de Descuentos (Coincide con los asserts del test antiguo)
        guardarConfig("DSCTO_MEMBRESIA_BRONCE", "0.02"); // 2% (Para que pase el assert 0.02)
        guardarConfig("DSCTO_MEMBRESIA_PLATA", "0.05");  // 5% (Para que pase el assert 0.05)
        guardarConfig("DSCTO_MEMBRESIA_ORO", "0.10");    // 10% (Para que pase el assert 0.10)

        // 2. Crear Cliente de prueba
        Cliente cliente = new Cliente();
        cliente.setNombres("Test Cliente");
        cliente.setApellidos("Fidelizacion");
        cliente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setDocIdentidad("12345678");
        cliente.setEmail("fidelizacion.test@pucp.edu.pe");
        cliente.setContrasena("clave123");
        cliente.setRol(Rol.CLIENTE);
        cliente.setNivel(TipoMembresia.BRONCE);
        clientePrueba = clienteRepository.save(cliente);

        // 3. Crear Reglas de Puntos (Necesarias para FK, aunque el valor lo lee de Config)
        reglaCompra = new ReglaPuntos();
        reglaCompra.setSolesPorPunto(1.0); // Valor referencial
        reglaCompra.setTipoRegla(TipoRegla.COMPRA);
        reglaCompra.setActivo(true);
        reglaCompra.setEstado("true");
        reglaCompra = reglaPuntosRepository.save(reglaCompra);

        reglaCanje = new ReglaPuntos();
        reglaCanje.setSolesPorPunto(10.0); // Valor referencial
        reglaCanje.setTipoRegla(TipoRegla.CANJE);
        reglaCanje.setActivo(true);
        reglaCanje.setEstado("true");
        reglaCanje = reglaPuntosRepository.save(reglaCanje);
        entityManager.flush();
        entityManager.clear();
        // 4. Crear Admin
        Administrador admin = new Administrador();
        admin.setNombres("Admin");
        admin.setApellidos("De Prueba");
        admin.setEmail("admin.test@pucp.edu.pe");
        admin.setContrasena("test123hashed");
        admin.setRol(Rol.ADMINISTRADOR);
        admin.setActivo(true);
        admin.setTipoDocumento(TipoDocumento.DNI);
        admin.setDocIdentidad("87654321");
        adminPrueba = administradorRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
        puntosRepository.deleteAll();
        canjeRepository.deleteAll();
        ordenCompraRepository.deleteAll();
        // Borra en orden inverso a la creación para evitar FK constraints
        clienteRepository.deleteAll();
        reglaPuntosRepository.deleteAll();
        configuracionRepository.deleteAll();
    }

    // Helper para guardar config
    private void guardarConfig(String key, String val) {
        ConfiguracionGlobal c = new ConfiguracionGlobal();
        c.setKey(key);
        c.setValue(val);
        c.setDescripcion("Test config");
        configuracionRepository.save(c);
    }

    @Test
    @DisplayName("Debe crear una regla de puntos correctamente")
    @WithMockUser(username = "admin.test@pucp.edu.pe")
    void debeCrearReglaPuntos() {
        ReglaPuntosRequestDTO request = new ReglaPuntosRequestDTO();
        request.setSolesPorPunto(15.0);
        request.setTipoRegla(TipoRegla.COMPRA);
        request.setActivo(true);
        request.setEstado("true");

        var resultado = fidelizacionService.crearReglaPuntos(request);

        assertNotNull(resultado.getIdRegla());
        assertEquals(15.0, resultado.getSolesPorPunto());
        assertEquals(TipoRegla.COMPRA, resultado.getTipoRegla());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("Debe generar puntos correctamente por una compra")
    void debeGenerarPuntosPorCompra() {
        // Given: Configuración dice 1 Sol = 1 Punto (guardado en setUp)

        // When: Compra de 100 soles
        Integer puntosGenerados = fidelizacionService.generarPuntosPorCompra(
                clientePrueba.getIdPersona(),
                100.0,
                1
        );

        // Forzar commit de la transacción REQUIRES_NEW
        entityManager.flush();
        entityManager.clear();

        // Verificar que retornó el valor correcto
        assertEquals(100, puntosGenerados, "Debería retornar 100 puntos generados");

        // Then: Debería tener 100 puntos (100 * 1)
        List<Puntos> puntos = puntosRepository.findByCliente_IdPersona(clientePrueba.getIdPersona());

        assertEquals(1, puntos.size(), "Debería haber exactamente 1 registro de puntos");
        assertEquals(100, puntos.get(0).getCantPuntos(), "El registro debería tener 100 puntos");
        assertEquals(TipoTransaccion.GANADO, puntos.get(0).getTipoTransaccion());
        assertTrue(puntos.get(0).getActivo());
    }

    @Test
    @DisplayName("Debe calcular puntos acumulados correctamente")
    void debeCalcularPuntosAcumulados() {
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 100);
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 50);

        Integer puntos = fidelizacionService.calcularPuntosAcumulados(clientePrueba.getIdPersona());

        assertEquals(150, puntos);
    }

    @Test
    @DisplayName("Debe crear un código promocional correctamente")
    @WithMockUser(username = "admin.test@pucp.edu.pe")
    void debeCrearCodigoPromocional() {
        CodigoPromocionalRequestDTO request = new CodigoPromocionalRequestDTO();
        request.setCodigo("TEST2024");
        request.setDescripcion("Descuento de prueba");
        request.setTipo(TipoCodigoPromocional.PORCENTAJE);
        request.setValor(15.0);
        request.setFechaFin(LocalDateTime.now().plusDays(30));
        request.setStock(100);
        request.setCantidadPorCliente(1);

        var resultado = fidelizacionService.crearCodigoPromocional(request);

        assertNotNull(resultado.getIdCodigoPromocional());
        assertEquals("TEST2024", resultado.getCodigo());
    }

    @Test
    @DisplayName("Debe fallar al crear código promocional duplicado")
    @WithMockUser(username = "admin.test@pucp.edu.pe")
    void debeFallarAlCrearCodigoPromocionalDuplicado() {
        CodigoPromocional codigo = new CodigoPromocional();
        codigo.setCodigo("DUPLICADO");
        codigo.setDescripcion("Test");
        codigo.setTipo(TipoCodigoPromocional.MONTO_FIJO);
        codigo.setValor(50.0);
        codigo.setStock(100);
        codigoPromocionalRepository.save(codigo);

        CodigoPromocionalRequestDTO request = new CodigoPromocionalRequestDTO();
        request.setCodigo("DUPLICADO");
        request.setDescripcion("Duplicado");
        request.setTipo(TipoCodigoPromocional.MONTO_FIJO);
        request.setValor(50.0);
        request.setStock(100);

        assertThrows(BusinessException.class, () -> {
            fidelizacionService.crearCodigoPromocional(request);
        });
    }

    @Test
    @DisplayName("Debe calcular descuento por membresía correctamente")
    void debeCalcularDescuentoPorMembresia() {
        // NOTA: En tu nueva lógica el descuento no depende de "cantidad de entradas", sino de la CONFIGURACIÓN.
        // El parámetro "cantidadEntradas" ahora es irrelevante, pero el método lo pide por firma.

        // Given - Configuración guardada en setUp:
        // BRONCE = 0.02, PLATA = 0.05, ORO = 0.10

        // When
        Double descuentoBronce = fidelizacionService.calcularDescuentoPorMembresia(TipoMembresia.BRONCE, 0);
        Double descuentoPlata = fidelizacionService.calcularDescuentoPorMembresia(TipoMembresia.PLATA, 0);
        Double descuentoOro = fidelizacionService.calcularDescuentoPorMembresia(TipoMembresia.ORO, 0);

        // Then
        assertEquals(0.02, descuentoBronce);
        assertEquals(0.05, descuentoPlata);
        assertEquals(0.10, descuentoOro);
    }

    // ... (El resto de tests de obtenerRegla, eliminar, etc. se mantienen igual) ...
    // Copia los métodos restantes tal cual los tenías abajo (listarPuntos, aplicarDescuento, etc.)

    @Test
    void debeListarPuntosPorCliente() {
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 50);
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 30);
        var resultado = fidelizacionService.listarPuntosPorCliente(clientePrueba.getIdPersona());
        assertEquals(2, resultado.size());
    }
}