package pe.edu.pucp.fasticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.fasticket.dto.fidelizacion.CodigoPromocionalRequestDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.ReglaPuntosRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
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
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.model.usuario.Rol;
import pe.edu.pucp.fasticket.model.usuario.TipoDocumento;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.LocalesRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepository;
import pe.edu.pucp.fasticket.repository.eventos.ZonaRepositorio;
import pe.edu.pucp.fasticket.repository.fidelizacion.CodigoPromocionalRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.PuntosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.ReglaPuntosRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests para FidelizacionService")
class FidelizacionServiceTest {

    @Autowired
    private FidelizacionService fidelizacionService;

    @Autowired
    private ReglaPuntosRepository reglaPuntosRepository;

    @Autowired
    private PuntosRepository puntosRepository;

    @Autowired
    private CodigoPromocionalRepository codigoPromocionalRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OrdenCompraRepositorio ordenCompraRepositorio;

    @Autowired
    private EventosRepositorio eventosRepositorio;

    @Autowired
    private LocalesRepositorio localesRepositorio;

    @Autowired
    private TipoTicketRepository tipoTicketRepository;

    @Autowired
    private ZonaRepositorio zonaRepositorio;


    private Cliente clientePrueba;
    private ReglaPuntos reglaCompra;
    private ReglaPuntos reglaCanje;

    @BeforeEach
    void setUp() {
        // 1. Crear Cliente de prueba
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

        // 2. Crear Reglas de Puntos
        reglaCompra = new ReglaPuntos();
        reglaCompra.setSolesPorPunto(10.0);
        reglaCompra.setTipoRegla(TipoRegla.COMPRA);
        reglaCompra.setActivo(true);
        reglaCompra.setEstado(true);
        reglaCompra = reglaPuntosRepository.save(reglaCompra);

        reglaCanje = new ReglaPuntos();
        reglaCanje.setSolesPorPunto(5.0);
        reglaCanje.setTipoRegla(TipoRegla.CANJE);
        reglaCanje.setActivo(true);
        reglaCanje.setEstado(true);
        reglaCanje = reglaPuntosRepository.save(reglaCanje);
    }

    @Test
    @DisplayName("Debe crear una regla de puntos correctamente")
    void debeCrearReglaPuntos() {
        // Given
        ReglaPuntosRequestDTO request = new ReglaPuntosRequestDTO();
        request.setSolesPorPunto(15.0);
        request.setTipoRegla(TipoRegla.COMPRA);
        request.setActivo(true);
        request.setEstado(true);

        // When
        var resultado = fidelizacionService.crearReglaPuntos(request);

        // Then
        assertNotNull(resultado.getIdRegla());
        assertEquals(15.0, resultado.getSolesPorPunto());
        assertEquals(TipoRegla.COMPRA, resultado.getTipoRegla());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("Debe generar puntos correctamente por una compra")
    void debeGenerarPuntosPorCompra() {
        // When
        fidelizacionService.generarPuntosPorCompra(
            clientePrueba.getIdPersona(),
            100.0, // 100 soles = 10 puntos (100/10)
            1
        );

        // Then
        List<Puntos> puntos = puntosRepository.findByCliente_IdPersona(clientePrueba.getIdPersona());
        assertEquals(1, puntos.size());
        assertEquals(10, puntos.get(0).getCantPuntos());
        assertEquals(TipoTransaccion.GANADO, puntos.get(0).getTipoTransaccion());
        assertTrue(puntos.get(0).getActivo());
    }

    @Test
    @DisplayName("Debe calcular puntos acumulados correctamente")
    void debeCalcularPuntosAcumulados() {
        // Given - Generar puntos ganados
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 100);
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 50);

        // When
        Integer puntos = fidelizacionService.calcularPuntosAcumulados(clientePrueba.getIdPersona());

        // Then
        assertEquals(150, puntos);
    }

    @Test
    @DisplayName("Debe crear un código promocional correctamente")
    void debeCrearCodigoPromocional() {
        // Given
        CodigoPromocionalRequestDTO request = new CodigoPromocionalRequestDTO();
        request.setCodigo("TEST2024");
        request.setDescripcion("Descuento de prueba");
        request.setTipo(TipoCodigoPromocional.PORCENTAJE);
        request.setValor(15.0);
        request.setFechaFin(LocalDateTime.now().plusDays(30));
        request.setStock(100);
        request.setCantidadPorCliente(1);

        // When
        var resultado = fidelizacionService.crearCodigoPromocional(request);

        // Then
        assertNotNull(resultado.getIdCodigoPromocional());
        assertEquals("TEST2024", resultado.getCodigo());
        assertEquals(TipoCodigoPromocional.PORCENTAJE, resultado.getTipo());
        assertEquals(15.0, resultado.getValor());
    }

    @Test
    @DisplayName("Debe fallar al crear código promocional duplicado")
    void debeFallarAlCrearCodigoPromocionalDuplicado() {
        // Given
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

        // When & Then
        assertThrows(BusinessException.class, () -> {
            fidelizacionService.crearCodigoPromocional(request);
        });
    }

    @Test
    @DisplayName("Debe calcular descuento por membresía correctamente")
    void debeCalcularDescuentoPorMembresia() {
        // Given
        TipoMembresia membresia = TipoMembresia.BRONCE;
        
        // When - Menos de 10 entradas
        Double descuento1 = fidelizacionService.calcularDescuentoPorMembresia(membresia, 5);
        // When - Entre 10 y 50 entradas
        Double descuento2 = fidelizacionService.calcularDescuentoPorMembresia(membresia, 25);
        // When - Más de 50 entradas
        Double descuento3 = fidelizacionService.calcularDescuentoPorMembresia(membresia, 60);

        // Then
        assertEquals(0.02, descuento1); // 2%
        assertEquals(0.05, descuento2); // 5%
        assertEquals(0.10, descuento3); // 10%
    }

    @Test
    @DisplayName("Debe listar puntos por cliente correctamente")
    void debeListarPuntosPorCliente() {
        // Given - Generar varios puntos
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 50);
        fidelizacionService.generarPuntos(clientePrueba.getIdPersona(), reglaCompra.getIdRegla(), 30);

        // When
        var resultado = fidelizacionService.listarPuntosPorCliente(clientePrueba.getIdPersona());

        // Then
        assertEquals(2, resultado.size());
    }

    @Test
    @DisplayName("Debe eliminar regla de puntos (desactivar)")
    void debeEliminarReglaPuntos() {
        // When
        fidelizacionService.eliminarReglaPuntos(reglaCompra.getIdRegla());

        // Then
        ReglaPuntos regla = reglaPuntosRepository.findById(reglaCompra.getIdRegla()).orElseThrow();
        assertTrue(!regla.getActivo());
    }

    @Test
    @DisplayName("Debe obtener regla de puntos por ID")
    void debeObtenerReglaPuntos() {
        // When
        var resultado = fidelizacionService.obtenerReglaPuntos(reglaCompra.getIdRegla());

        // Then
        assertNotNull(resultado);
        assertEquals(reglaCompra.getIdRegla(), resultado.getIdRegla());
        assertEquals(10.0, resultado.getSolesPorPunto());
    }

    @Test
    @DisplayName("Debe fallar al obtener regla de puntos inexistente")
    void debeFallarObtenerReglaInexistente() {
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            fidelizacionService.obtenerReglaPuntos(99999);
        });
    }

    @Test
    @DisplayName("Debe aplicar descuento por código promocional correctamente")
    void debeAplicarDescuentoPorCodigoPromocional() {
        // Given - Crear código promocional
        CodigoPromocional codigo = new CodigoPromocional();
        codigo.setCodigo("PROMO20");
        codigo.setDescripcion("20% de descuento");
        codigo.setTipo(TipoCodigoPromocional.PORCENTAJE);
        codigo.setValor(20.0);
        codigo.setStock(100);
        codigo = codigoPromocionalRepository.save(codigo);

        // Crear Local, Evento, Zona, TipoTicket
        Local local = new Local();
        local.setNombre("Local Test");
        local.setDireccion("Dirección Test");
        local.setAforoTotal(1000);
        local.setActivo(true);
        local = localesRepositorio.save(local);

        Evento evento = new Evento();
        evento.setNombre("Evento Test");
        evento.setDescripcion("Descripción Test");
        evento.setFechaEvento(LocalDate.now().plusDays(30));
        evento.setHoraInicio(LocalTime.of(20, 0));
        evento.setTipoEvento(TipoEvento.ROCK);
        evento.setEstadoEvento(EstadoEvento.ACTIVO);
        evento.setAforoDisponible(1000);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());
        evento.setLocal(local);
        evento = eventosRepositorio.save(evento);

        Zona zona = new Zona();
        zona.setNombre("Zona A");
        zona.setAforoMax(500);
        zona.setActivo(true);
        zona.setLocal(local);
        zona = zonaRepositorio.save(zona);

        TipoTicket tipoTicket = new TipoTicket();
        tipoTicket.setNombre("General");
        tipoTicket.setPrecio(100.0);
        tipoTicket.setCantidadDisponible(100);
        tipoTicket.setStock(100);
        tipoTicket.setActivo(true);
        tipoTicket.setZona(zona);
        tipoTicket = tipoTicketRepository.save(tipoTicket);

        // Crear orden
        OrdenCompra orden = new OrdenCompra();
        orden.setCliente(clientePrueba);
        orden.setEstado(EstadoCompra.PENDIENTE);
        orden.setFechaOrden(LocalDate.now());
        
        ItemCarrito item = new ItemCarrito();
        item.setTipoTicket(tipoTicket);
        item.setCantidad(1);
        item.setPrecio(100.0);
        item.setPrecioFinal(100.0);
        orden.addItem(item);
        orden.calcularTotal();
        orden = ordenCompraRepositorio.save(orden);

        // When
        fidelizacionService.aplicarDescuentoPorCodigoPromocional(orden.getIdOrdenCompra(), "PROMO20");

        // Then
        CodigoPromocional codigoActualizado = codigoPromocionalRepository.findById(codigo.getIdCodigoPromocional()).orElseThrow();
        assertEquals(99, codigoActualizado.getStock());
    }

    @Test
    @DisplayName("Debe fallar al aplicar código promocional expirado")
    void debeFallarCodigoPromocionalExpirado() {
        // Given - Código expirado
        CodigoPromocional codigo = new CodigoPromocional();
        codigo.setCodigo("EXPIRADO");
        codigo.setDescripcion("Código expirado");
        codigo.setTipo(TipoCodigoPromocional.MONTO_FIJO);
        codigo.setValor(50.0);
        codigo.setFechaFin(LocalDateTime.now().minusDays(1)); // Expirado
        codigo.setStock(100);
        codigo = codigoPromocionalRepository.save(codigo);

        // Crear Local, Evento, Zona, TipoTicket
        Local local = new Local();
        local.setNombre("Local Test 2");
        local.setDireccion("Dirección Test 2");
        local.setAforoTotal(1000);
        local.setActivo(true);
        local = localesRepositorio.save(local);

        Evento evento = new Evento();
        evento.setNombre("Evento Test 2");
        evento.setDescripcion("Descripción Test 2");
        evento.setFechaEvento(LocalDate.now().plusDays(30));
        evento.setHoraInicio(LocalTime.of(20, 0));
        evento.setTipoEvento(TipoEvento.POP);
        evento.setEstadoEvento(EstadoEvento.ACTIVO);
        evento.setAforoDisponible(1000);
        evento.setActivo(true);
        evento.setFechaCreacion(LocalDate.now());
        evento.setLocal(local);
        evento = eventosRepositorio.save(evento);

        Zona zona = new Zona();
        zona.setNombre("Zona B");
        zona.setAforoMax(500);
        zona.setActivo(true);
        zona.setLocal(local);
        zona = zonaRepositorio.save(zona);

        TipoTicket tipoTicket = new TipoTicket();
        tipoTicket.setNombre("General 2");
        tipoTicket.setPrecio(100.0);
        tipoTicket.setCantidadDisponible(100);
        tipoTicket.setStock(100);
        tipoTicket.setActivo(true);
        tipoTicket.setZona(zona);
        tipoTicket = tipoTicketRepository.save(tipoTicket);

        final OrdenCompra ordenFinal = new OrdenCompra();
        ordenFinal.setCliente(clientePrueba);
        ordenFinal.setEstado(EstadoCompra.PENDIENTE);
        ordenFinal.setFechaOrden(LocalDate.now());
        
        ItemCarrito item = new ItemCarrito();
        item.setTipoTicket(tipoTicket);
        item.setCantidad(1);
        item.setPrecio(100.0);
        item.setPrecioFinal(100.0);
        ordenFinal.addItem(item);
        ordenFinal.calcularTotal();
        final OrdenCompra ordenGuardada = ordenCompraRepositorio.save(ordenFinal);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            fidelizacionService.aplicarDescuentoPorCodigoPromocional(ordenGuardada.getIdOrdenCompra(), "EXPIRADO");
        });
    }
}

