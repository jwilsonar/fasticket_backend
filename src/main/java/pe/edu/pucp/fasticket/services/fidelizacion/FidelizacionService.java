package pe.edu.pucp.fasticket.services.fidelizacion;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.fidelizacion.CanjeDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.CanjeRequestDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.CodigoPromocionalDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.CodigoPromocionalRequestDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.PuntosDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.ReglaPuntosDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.ReglaPuntosRequestDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.ConfiguracionGlobal;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.fidelizacion.Canje;
import pe.edu.pucp.fasticket.model.fidelizacion.CodigoPromocional;
import pe.edu.pucp.fasticket.model.fidelizacion.DescuentosRealizados;
import pe.edu.pucp.fasticket.model.fidelizacion.Puntos;
import pe.edu.pucp.fasticket.model.fidelizacion.ReglaPuntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoCodigoPromocional;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoRegla;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoTransaccion;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.ConfiguracionRepository;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.fidelizacion.CanjeRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.CodigoPromocionalRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.DescuentosRealizadosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.PuntosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.ReglaPuntosRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FidelizacionService {

    private final ReglaPuntosRepository reglaPuntosRepository;
    private final PuntosRepository puntosRepository;
    private final CanjeRepository canjeRepository;
    private final CodigoPromocionalRepository codigoPromocionalRepository;
    private final DescuentosRealizadosRepository descuentosRealizadosRepository;
    private final ClienteRepository clienteRepository;
    private final OrdenCompraRepositorio ordenCompraRepositorio;

    private final ConfiguracionRepository configuracionRepository;

    private final AuditLogService auditLogService;
    private final AdministradorRepository administradorRepository;

    // ============ 1. REGLAS DE PUNTOS (CRUD) ============

    public List<ReglaPuntosDTO> listarReglasPuntos() {
        return reglaPuntosRepository.findAll().stream()
                .map(ReglaPuntosDTO::new)
                .collect(Collectors.toList());
    }

    public List<ReglaPuntosDTO> listarReglasActivas() {
        return reglaPuntosRepository.findByActivoTrue().stream()
                .map(ReglaPuntosDTO::new)
                .collect(Collectors.toList());
    }

    public ReglaPuntosDTO obtenerReglaPuntos(Integer id) {
        ReglaPuntos regla = reglaPuntosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de puntos no encontrada con ID: " + id));
        return new ReglaPuntosDTO(regla);
    }

    @Transactional
    public ReglaPuntosDTO crearReglaPuntos(ReglaPuntosRequestDTO request) {
        Administrador adminActual = getAdminActual();

        ReglaPuntos regla = new ReglaPuntos();
        regla.setSolesPorPunto(request.getSolesPorPunto());
        regla.setTipoRegla(request.getTipoRegla());
        regla.setActivo(request.getActivo());
        regla.setEstado(request.getEstado());

        ReglaPuntos guardada = reglaPuntosRepository.save(regla);

        // Auditoría
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") CREÓ la Regla de Puntos ID: " + guardada.getIdRegla();
            auditLogService.registrarAuditoria(adminActual, "CREAR_REGLA_PUNTOS", "FidelizacionService", detalle);
        } catch (Exception e) { log.error("Error auditoría", e); }

        log.info("Regla de puntos creada con ID: {}", guardada.getIdRegla());
        return new ReglaPuntosDTO(guardada);
    }

    @Transactional
    public ReglaPuntosDTO actualizarReglaPuntos(Integer id, ReglaPuntosRequestDTO request) {
        Administrador adminActual = getAdminActual();

        ReglaPuntos regla = reglaPuntosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de puntos no encontrada con ID: " + id));

        regla.setSolesPorPunto(request.getSolesPorPunto());
        regla.setTipoRegla(request.getTipoRegla());
        regla.setActivo(request.getActivo());
        regla.setEstado(request.getEstado());

        ReglaPuntos actualizada = reglaPuntosRepository.save(regla);

        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") ACTUALIZÓ la Regla de Puntos ID: " + id;
            auditLogService.registrarAuditoria(adminActual, "ACTUALIZAR_REGLA_PUNTOS", "FidelizacionService", detalle);
        } catch (Exception e) { log.error("Error auditoría", e); }

        return new ReglaPuntosDTO(actualizada);
    }

    @Transactional
    public void eliminarReglaPuntos(Integer id) {
        Administrador adminActual = getAdminActual();
        ReglaPuntos regla = reglaPuntosRepository.findById(id).orElseThrow();
        regla.setActivo(false);
        reglaPuntosRepository.save(regla);

        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") DESACTIVÓ la Regla de Puntos ID: " + id;
            auditLogService.registrarAuditoria(adminActual, "DESACTIVAR_REGLA_PUNTOS", "FidelizacionService", detalle);
        } catch (Exception e) { log.error("Error auditoría", e); }
    }

    // ============ PUNTOS ============

    public List<PuntosDTO> listarPuntosPorCliente(Integer idCliente) {
        return puntosRepository.findByCliente_IdPersona(idCliente).stream()
                .map(PuntosDTO::new)
                .collect(Collectors.toList());
    }

    public List<PuntosDTO> listarPuntosActivosPorCliente(Integer idCliente) {
        return puntosRepository.findByCliente_IdPersonaAndActivoTrue(idCliente).stream()
                .map(PuntosDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void borrarRegistroPuntos(Integer idCliente, Integer idPuntos) {
        log.warn("Solicitud de ANULACIÓN de registro de puntos ID: {} para cliente ID: {}", idPuntos, idCliente);
        Administrador adminActual = getAdminActual(); // Obtener admin

        Puntos puntos = puntosRepository.findById(idPuntos)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de puntos no encontrado con ID: " + idPuntos));
        Cliente cliente = puntos.getCliente();
        if (!cliente.getIdPersona().equals(idCliente)) {
            throw new SecurityException("Este registro de puntos no pertenece al cliente especificado.");
        }
        if (!puntos.getActivo()) {
            throw new BusinessException("Solo se pueden anular registros que estén ACTIVOS.");
        }
        int puntosActuales = (cliente.getPuntosAcumulados() != null) ? cliente.getPuntosAcumulados() : 0;
        int efectoNeto = 0;
        if (puntos.getTipoTransaccion() == TipoTransaccion.GANADO) {
            efectoNeto = puntos.getCantPuntos();
        } else if (puntos.getTipoTransaccion() == TipoTransaccion.PERDIDO) {
            efectoNeto = -puntos.getCantPuntos();
        }
        int puntosNuevos = puntosActuales - efectoNeto;
        if (puntosNuevos < 0) {
            throw new BusinessException("No se puede anular este registro porque resultaría en un saldo de puntos negativo.");
        }
        cliente.setPuntosAcumulados(puntosNuevos);
        clienteRepository.save(cliente);
        puntos.setActivo(false);
        puntosRepository.save(puntos);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") BORRÓ el registro de Puntos ID: " + idPuntos + " (Cliente Afectado ID: " + idCliente + ")";
            auditLogService.registrarAuditoria(adminActual, "BORRAR_REGISTRO_PUNTOS", "FidelizacionService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (BORRAR_REGISTRO_PUNTOS): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Registro de puntos ID: {} ANULADO. Saldo de cliente ID {} actualizado a {}.", idPuntos, idCliente, puntosNuevos);
    }

    @Transactional
    public Integer calcularPuntosAcumulados(Integer idCliente) {
        Integer puntos = puntosRepository.calcularPuntosAcumulados(idCliente, TipoTransaccion.GANADO);
        return puntos != null ? puntos : 0;
    }

    @Transactional
    public PuntosDTO generarPuntos(Integer idCliente, Integer idRegla, Integer cantidad) {
        ReglaPuntos regla = reglaPuntosRepository.findById(idRegla).orElseThrow();
        Cliente cliente = clienteRepository.findById(idCliente).orElseThrow();

        Puntos puntos = new Puntos();
        puntos.setCantPuntos(cantidad);
        puntos.setTipoTransaccion(TipoTransaccion.GANADO);
        puntos.setFechaTransaccion(LocalDate.now());
        puntos.setCliente(cliente);
        puntos.setReglaPuntos(regla);
        puntos.setActivo(true);
        if (regla.getTipoRegla() == TipoRegla.COMPRA) {
            puntos.setFechaVencimiento(LocalDate.now().plusYears(1));
        }
        return new PuntosDTO(puntosRepository.save(puntos));
    }

    // ============ CANJES ============

    public List<CanjeDTO> listarCanjesPorCliente(Integer idCliente) {
        return canjeRepository.findByOrdenCompra_Cliente_IdPersona(idCliente).stream()
                .map(CanjeDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public CanjeDTO realizarCanje(CanjeRequestDTO request) {
        // Validaciones
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        OrdenCompra orden = ordenCompraRepositorio.findById(request.getIdOrdenCompra())
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (!orden.getCliente().getIdPersona().equals(cliente.getIdPersona())) {
            throw new BusinessException("La orden no pertenece al cliente");
        }
        if (orden.getEstado() != pe.edu.pucp.fasticket.model.compra.EstadoCompra.PENDIENTE) {
            throw new BusinessException("Solo se pueden canjear puntos en órdenes pendientes");
        }

        orden.calcularTotal();
        Double totalOrden = orden.getTotal();

        // --- LÓGICA MATEMÁTICA CORREGIDA ---
        // Leemos el factor de Configuración Global (Ej: 10 puntos para 1 sol)
        int puntosParaUnSol = Integer.parseInt(getConfig("PUNTOS_PARA_DESCONTAR_UN_SOL", "10"));

        // Usamos la regla de BD solo por integridad referencial (FK)
        ReglaPuntos reglaReferencia = reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.CANJE)
                .stream().findFirst().orElseThrow(() -> new BusinessException("Error interno: Falta regla CANJE"));

        // Fórmula Multiplicación: TotalSoles * PuntosPorSol
        // Ej: 100 Soles * 10 = 1000 Puntos necesarios
        Integer puntosNecesarios = (int) Math.ceil(totalOrden * puntosParaUnSol);

        // Validaciones de Saldo
        int saldoActual = cliente.getPuntosAcumulados() != null ? cliente.getPuntosAcumulados() : 0;

        if (saldoActual < puntosNecesarios) {
            throw new BusinessException("Puntos insuficientes. Tienes " + saldoActual + ", necesitas " + puntosNecesarios);
        }
        if (!request.getPuntosCanje().equals(puntosNecesarios)) {
            throw new BusinessException("Debes canjear exactamente " + puntosNecesarios + " puntos para esta orden.");
        }
        if (!request.getMontoDescuento().equals(totalOrden)) {
            throw new BusinessException("El monto de descuento debe cubrir el total de la orden.");
        }

        // Ejecutar Canje (Puntos Perdidos)
        Puntos puntosCanje = new Puntos();
        puntosCanje.setCantPuntos(puntosNecesarios);
        puntosCanje.setTipoTransaccion(TipoTransaccion.PERDIDO);
        puntosCanje.setFechaTransaccion(LocalDate.now());
        puntosCanje.setCliente(cliente);
        puntosCanje.setReglaPuntos(reglaReferencia);
        puntosCanje.setActivo(true);
        Puntos puntosGuardados = puntosRepository.save(puntosCanje);

        // Actualizar Saldo Cliente
        cliente.setPuntosAcumulados(saldoActual - puntosNecesarios);
        // ¡NO ACTUALIZAMOS NIVEL AQUÍ! (Protección de Status)
        clienteRepository.save(cliente);

        // Aplicar Descuento
        orden.setDescuentoPorCanje(totalOrden);
        orden.aplicarDescuentoYRecalcular();
        ordenCompraRepositorio.save(orden);

        Canje canje = new Canje();
        canje.setFechaCanje(LocalDate.now());
        canje.setOrdenCompra(orden);
        canje.setPuntos(puntosGuardados);

        log.info("Canje Exitoso: Orden {} cubierta con {} puntos.", orden.getIdOrdenCompra(), puntosNecesarios);
        return new CanjeDTO(canjeRepository.save(canje));
    }

    // ============ CÓDIGOS PROMOCIONALES ============

    public List<CodigoPromocionalDTO> listarCodigosPromocionales() {
        return codigoPromocionalRepository.findAll().stream().map(CodigoPromocionalDTO::new).collect(Collectors.toList());
    }
    public CodigoPromocionalDTO obtenerCodigoPromocional(Integer id) {
        return new CodigoPromocionalDTO(codigoPromocionalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Código no encontrado")));
    }
    public CodigoPromocionalDTO obtenerPorCodigo(String codigo) {
        return new CodigoPromocionalDTO(codigoPromocionalRepository.findByCodigo(codigo).orElseThrow(() -> new ResourceNotFoundException("Código no encontrado")));
    }

    @Transactional
    public CodigoPromocionalDTO crearCodigoPromocional(CodigoPromocionalRequestDTO request) {
        Administrador admin = getAdminActual();
        if (codigoPromocionalRepository.existsByCodigo(request.getCodigo())) throw new BusinessException("Código duplicado");

        CodigoPromocional c = new CodigoPromocional();
        c.setCodigo(request.getCodigo());
        c.setDescripcion(request.getDescripcion());
        c.setFechaFin(request.getFechaFin());
        c.setTipo(request.getTipo());
        c.setValor(request.getValor());
        c.setStock(request.getStock());
        c.setCantidadPorCliente(request.getCantidadPorCliente());
        c.setActivo(request.getActivo() != null ? request.getActivo() : true);
        CodigoPromocional guardado = codigoPromocionalRepository.save(c);

        try {
            auditLogService.registrarAuditoria(admin, "CREAR_CODIGO_PROMO", "FidelizacionService", "Creó código: " + guardado.getCodigo());
        } catch (Exception e) {}

        return new CodigoPromocionalDTO(guardado);
    }

    @Transactional
    public CodigoPromocionalDTO actualizarCodigoPromocional(Integer id, CodigoPromocionalRequestDTO request) {
        Administrador adminActual = getAdminActual(); // Obtener admin

        CodigoPromocional codigo = codigoPromocionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado con ID: " + id));

        // Verificar si el código ya existe en otra entidad
        if (!codigo.getCodigo().equals(request.getCodigo()) && codigoPromocionalRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe un código promocional con el código: " + request.getCodigo());
        }

        codigo.setCodigo(request.getCodigo());
        codigo.setDescripcion(request.getDescripcion());
        codigo.setFechaFin(request.getFechaFin());
        codigo.setTipo(request.getTipo());
        codigo.setValor(request.getValor());
        codigo.setStock(request.getStock());
        codigo.setCantidadPorCliente(request.getCantidadPorCliente());
        if (request.getActivo() != null) {
            codigo.setActivo(request.getActivo());
        }
        CodigoPromocional actualizado = codigoPromocionalRepository.save(codigo);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") ACTUALIZÓ el Código Promocional: " + actualizado.getCodigo() + " (ID: " + id + ")";
            auditLogService.registrarAuditoria(adminActual, "ACTUALIZAR_CODIGO_PROMO", "FidelizacionService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (ACTUALIZAR_CODIGO_PROMO): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Código promocional actualizado con ID: {}", actualizado.getIdCodigoPromocional());
        return new CodigoPromocionalDTO(actualizado);
    }

    @Transactional
    public void eliminarCodigoPromocional(Integer id) {
        Administrador adminActual = getAdminActual(); // Obtener admin

        CodigoPromocional codigo = codigoPromocionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado con ID: " + id));

        codigo.setActivo(false);
        codigoPromocionalRepository.save(codigo);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") DESACTIVÓ el Código Promocional: " + codigo.getCodigo() + " (ID: " + id + ")";
            auditLogService.registrarAuditoria(adminActual, "DESACTIVAR_CODIGO_PROMO", "FidelizacionService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (DESACTIVAR_CODIGO_PROMO): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Código promocional eliminado con ID: {}", id);
    }

    // ============ MÉTODOS AUXILIARES ============

    /**
     * Calcula descuento dinámico leyendo configuración (Para el Checkout)
     */
    @Transactional
    public Double calcularDescuentoPorMembresia(TipoMembresia tipoMembresia, Integer cantidadEntradas) {
        // La configuración espera valores decimales (0.10 para 10%)
        // Claves esperadas en BD: DSCTO_MEMBRESIA_BRONCE, DSCTO_MEMBRESIA_PLATA, DSCTO_MEMBRESIA_ORO
        String key = "DSCTO_MEMBRESIA_" + tipoMembresia.name();
        String valorConfig = getConfig(key, "0.0");

        return Double.parseDouble(valorConfig);
    }

    @Transactional
    public void generarPuntosPorCompra(Integer idCliente, Double montoTotal, Integer idOrdenCompra) {
        // Lee Configuración Global (1 Sol = 1 Punto)
        int puntosPorSol = Integer.parseInt(getConfig("PUNTOS_POR_MONEDA", "1"));
        int puntosGenerados = (int) (montoTotal * puntosPorSol);

        if (puntosGenerados <= 0) return;

        ReglaPuntos reglaBase = reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.COMPRA)
                .stream().findFirst().orElse(null);

        if (reglaBase != null) {
            generarPuntos(idCliente, reglaBase.getIdRegla(), puntosGenerados);

            Cliente cliente = clienteRepository.findById(idCliente).orElseThrow();
            int nuevoAcumulado = (cliente.getPuntosAcumulados() != null ? cliente.getPuntosAcumulados() : 0) + puntosGenerados;
            cliente.setPuntosAcumulados(nuevoAcumulado);

            // SÍ actualiza nivel (Ganancia)
            actualizarNivelCliente(cliente, nuevoAcumulado);

            clienteRepository.save(cliente);
            log.info("Orden {}: Cliente {} ganó {} puntos.", idOrdenCompra, idCliente, puntosGenerados);
        }
    }

    /**
     * Revertir puntos cuando se cancela una orden
     */
    @Transactional
    public void revertirPuntosPorAnulacion(OrdenCompra orden) {
        int puntosPorSol = Integer.parseInt(getConfig("PUNTOS_POR_MONEDA", "1"));
        int puntosARestar = (int) (orden.getTotal() * puntosPorSol);

        if (puntosARestar <= 0) return;

        Cliente cliente = orden.getCliente();
        ReglaPuntos reglaBase = reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.COMPRA)
                .stream().findFirst().orElse(null);

        if (reglaBase != null) {
            Puntos reverso = new Puntos();
            reverso.setCantPuntos(puntosARestar);
            reverso.setTipoTransaccion(TipoTransaccion.PERDIDO);
            reverso.setFechaTransaccion(LocalDate.now());
            reverso.setCliente(cliente);
            reverso.setReglaPuntos(reglaBase);
            reverso.setActivo(true);
            puntosRepository.save(reverso);
        }

        int nuevoAcumulado = Math.max((cliente.getPuntosAcumulados() != null ? cliente.getPuntosAcumulados() : 0) - puntosARestar, 0);
        cliente.setPuntosAcumulados(nuevoAcumulado);

        // SÍ actualiza nivel (Pérdida por anulación)
        actualizarNivelCliente(cliente, nuevoAcumulado);

        clienteRepository.save(cliente);
        log.info("Reversión: Cliente {} perdió {} puntos.", cliente.getIdPersona(), puntosARestar);
    }

    /**
     * Lógica centralizada para determinar el nivel (Bronze/Silver/Gold)
     */
    private void actualizarNivelCliente(Cliente cliente, int puntosTotales) {
        int umbralSilver = Integer.parseInt(getConfig("NIVEL_SILVER_MIN_PUNTOS", "1000"));
        int umbralGold = Integer.parseInt(getConfig("NIVEL_GOLD_MIN_PUNTOS", "5000"));

        TipoMembresia nivelActual = cliente.getNivel();
        TipoMembresia nuevoNivel = TipoMembresia.BRONCE;

        if (puntosTotales >= umbralGold) {
            nuevoNivel = TipoMembresia.ORO;
        } else if (puntosTotales >= umbralSilver) {
            nuevoNivel = TipoMembresia.PLATA;
        }

        if (nuevoNivel != nivelActual) {
            cliente.setNivel(nuevoNivel);
            log.info("Cambio de Nivel: Cliente {} pasó de {} a {}", cliente.getIdPersona(), nivelActual, nuevoNivel);
        }
    }

    @Transactional
    public void aplicarDescuentoPorCodigoPromocional(Integer idOrdenCompra, String codigo) {
        CodigoPromocional codigoPromo = codigoPromocionalRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado: " + codigo));
        if (Boolean.FALSE.equals(codigoPromo.getActivo())) {
            throw new BusinessException("El código promocional está desactivado o inhabilitado.");
        }
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrdenCompra)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + idOrdenCompra));

        // Validar que la orden esté en estado PENDIENTE
        if (orden.getEstado() != pe.edu.pucp.fasticket.model.compra.EstadoCompra.PENDIENTE) {
            throw new BusinessException("Solo se pueden aplicar códigos promocionales en órdenes pendientes");
        }

        // Validar que no haya canje aplicado (mutuamente excluyente)
        if (orden.getDescuentoPorCanje() != null && orden.getDescuentoPorCanje() > 0) {
            throw new BusinessException("No se pueden aplicar códigos promocionales cuando se ha canjeado puntos. Los descuentos son mutuamente excluyentes.");
        }

        // Validar stock
        if (codigoPromo.getStock() <= 0) {
            throw new BusinessException("El código promocional no tiene stock disponible");
        }

        // Validar vigencia
        if (codigoPromo.getFechaFin() != null && codigoPromo.getFechaFin().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("El código promocional ha expirado");
        }

        // Aplicar descuento
        Double descuento = 0.0;
        if (codigoPromo.getTipo() == TipoCodigoPromocional.PORCENTAJE) {
            descuento = orden.getSubtotal() * (codigoPromo.getValor() / 100.0);
        } else {
            descuento = codigoPromo.getValor();
        }

        // Registrar descuento
        DescuentosRealizados descuentoRealizado = new DescuentosRealizados();
        descuentoRealizado.setCodigoPromocional(codigoPromo);
        descuentoRealizado.setOrdenCompra(orden);
        descuentoRealizado.setValor(descuento);
        descuentosRealizadosRepository.save(descuentoRealizado);

        // Actualizar stock
        codigoPromo.setStock(codigoPromo.getStock() - 1);
        codigoPromocionalRepository.save(codigoPromo);

        log.info("Descuento aplicado: {} por código promocional: {}", descuento, codigo);
    }

    // --- NUEVO MÉTODO HELPER PARA AUDITORÍA ---
    private Administrador getAdminActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No hay un usuario autenticado para la auditoría.");
        }
        String username = authentication.getName();
        return administradorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado para auditoría con username: " + username));
    }

    @Transactional
    public Double validarYCalcularDescuento(String codigo, Double subtotal) {
        CodigoPromocional codigoPromo = codigoPromocionalRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado: " + codigo));
        if (Boolean.FALSE.equals(codigoPromo.getActivo())) {
            throw new BusinessException("El código promocional está inhabilitado.");
        }
        if (codigoPromo.getStock() <= 0) {
            throw new BusinessException("El código promocional no tiene stock disponible");
        }
        if (codigoPromo.getFechaFin() != null && codigoPromo.getFechaFin().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("El código promocional ha expirado");
        }
        Double descuento = 0.0;
        if (codigoPromo.getTipo() == TipoCodigoPromocional.PORCENTAJE) {
            descuento = subtotal * (codigoPromo.getValor() / 100.0);
        } else {
            descuento = codigoPromo.getValor();
        }
        codigoPromo.setStock(codigoPromo.getStock() - 1);
        codigoPromocionalRepository.save(codigoPromo);

        log.info("Descuento calculado: {} por código promocional: {}", descuento, codigo);
        return descuento;
    }

    // Helper para leer config seguro
    private String getConfig(String key, String defaultValue) {
        return configuracionRepository.findById(key)
                .map(ConfiguracionGlobal::getValue)
                .orElse(defaultValue);
    }
}

