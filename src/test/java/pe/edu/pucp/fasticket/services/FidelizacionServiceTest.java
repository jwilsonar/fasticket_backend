package pe.edu.pucp.fasticket.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.fidelizacion.*;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.PuntosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.ReglaPuntosRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FidelizacionServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private ConfiguracionRepository configuracionRepository;
    @Mock private ReglaPuntosRepository reglaPuntosRepository;
    @Mock private PuntosRepository puntosRepository;

    @InjectMocks
    private FidelizacionService fidelizacionService;

    private Cliente cliente;
    private ConfiguracionGlobal configPuntos;
    private ConfiguracionGlobal configUmbralGold;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setIdPersona(1);
        cliente.setPuntosAcumulados(0);
        cliente.setNivel(TipoMembresia.BRONCE);

        // Simulamos que la configuración dice: 10 Puntos por cada 1 Sol
        configPuntos = new ConfiguracionGlobal();
        configPuntos.setValue("10");

        // Simulamos que el umbral para ser GOLD es 5000 puntos
        configUmbralGold = new ConfiguracionGlobal();
        configUmbralGold.setValue("5000");
    }

    @Test
    void generarPuntos_DebeSubirDeNivel_SiSuperaUmbral() {
        // GIVEN
        Double montoCompra = 600.00; // 6000 puntos
        ReglaPuntos reglaBase = new ReglaPuntos();
        reglaBase.setIdRegla(1);
        reglaBase.setTipoRegla(TipoRegla.COMPRA); // Aseguramos el tipo

        // 1. Configuración de Puntos y Umbrales
        when(configuracionRepository.findById("PUNTOS_POR_MONEDA")).thenReturn(Optional.of(configPuntos));
        when(configuracionRepository.findById("NIVEL_SILVER_MIN_PUNTOS")).thenReturn(Optional.of(new ConfiguracionGlobal(){{setValue("1000");}}));
        when(configuracionRepository.findById("NIVEL_GOLD_MIN_PUNTOS")).thenReturn(Optional.of(configUmbralGold));

        // 2. Mockear búsqueda de cliente
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));

        // 3. Mockear búsqueda de reglas (LISTA) - Esto ya lo tenías
        when(reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.COMPRA)).thenReturn(List.of(reglaBase));

        // 4. ¡LA LÍNEA QUE FALTABA! Mockear búsqueda por ID (findById)
        // El método interno 'generarPuntos' hace un findById, necesitamos simularlo:
        when(reglaPuntosRepository.findById(1)).thenReturn(Optional.of(reglaBase));

        // 5. Mockear el guardado final
        when(puntosRepository.save(any(Puntos.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        fidelizacionService.generarPuntosPorCompra(1, montoCompra, 100);

        // THEN
        // Verificamos que se guardó el registro de puntos
        verify(puntosRepository).save(any(Puntos.class));

        // Verificamos que el cliente ahora tiene 6000 puntos
        assertEquals(6000, cliente.getPuntosAcumulados());

        // Verificamos que subió a ORO automáticamente
        assertEquals(TipoMembresia.ORO, cliente.getNivel());
    }

    @Test
    void revertirPuntos_DebeRestarPuntos_AlCancelarOrden() {
        // GIVEN
        cliente.setPuntosAcumulados(6000); // El cliente ya tenía puntos

        OrdenCompra orden = new OrdenCompra();
        orden.setIdOrdenCompra(99);
        orden.setTotal(100.0); // 100 soles * 10 = 1000 puntos a restar
        orden.setCliente(cliente);

        when(configuracionRepository.findById("PUNTOS_POR_MONEDA")).thenReturn(Optional.of(configPuntos));
        when(reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.COMPRA)).thenReturn(List.of(new ReglaPuntos()));
        // Mock umbrales para recalcular nivel
        when(configuracionRepository.findById("NIVEL_SILVER_MIN_PUNTOS")).thenReturn(Optional.of(new ConfiguracionGlobal(){{setValue("1000");}}));
        when(configuracionRepository.findById("NIVEL_GOLD_MIN_PUNTOS")).thenReturn(Optional.of(configUmbralGold));

        // WHEN
        fidelizacionService.revertirPuntosPorAnulacion(orden);

        // THEN
        // Tenía 6000, le quitamos 1000, debe quedar en 5000
        assertEquals(5000, cliente.getPuntosAcumulados());

        // Verificamos que se creó un registro de tipo PERDIDO
        verify(puntosRepository).save(argThat(puntos ->
                puntos.getTipoTransaccion() == TipoTransaccion.PERDIDO &&
                        puntos.getCantPuntos() == 1000
        ));
    }
}