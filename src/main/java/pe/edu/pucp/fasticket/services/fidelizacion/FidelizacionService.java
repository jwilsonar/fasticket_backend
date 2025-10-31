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
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.fidelizacion.CanjeRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.CodigoPromocionalRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.DescuentosRealizadosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.PuntosRepository;
import pe.edu.pucp.fasticket.repository.fidelizacion.ReglaPuntosRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;

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

    // ============ REGLAS DE PUNTOS ============
    
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
        ReglaPuntos regla = new ReglaPuntos();
        regla.setSolesPorPunto(request.getSolesPorPunto());
        regla.setTipoRegla(request.getTipoRegla());
        regla.setActivo(request.getActivo());
        regla.setEstado(request.getEstado());
        
        ReglaPuntos guardada = reglaPuntosRepository.save(regla);
        log.info("Regla de puntos creada con ID: {}", guardada.getIdRegla());
        return new ReglaPuntosDTO(guardada);
    }

    @Transactional
    public ReglaPuntosDTO actualizarReglaPuntos(Integer id, ReglaPuntosRequestDTO request) {
        ReglaPuntos regla = reglaPuntosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de puntos no encontrada con ID: " + id));
        
        regla.setSolesPorPunto(request.getSolesPorPunto());
        regla.setTipoRegla(request.getTipoRegla());
        regla.setActivo(request.getActivo());
        regla.setEstado(request.getEstado());
        
        ReglaPuntos actualizada = reglaPuntosRepository.save(regla);
        log.info("Regla de puntos actualizada con ID: {}", actualizada.getIdRegla());
        return new ReglaPuntosDTO(actualizada);
    }

    @Transactional
    public void eliminarReglaPuntos(Integer id) {
        ReglaPuntos regla = reglaPuntosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de puntos no encontrada con ID: " + id));
        
        regla.setActivo(false);
        reglaPuntosRepository.save(regla);
        log.info("Regla de puntos desactivada con ID: {}", id);
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
    public Integer calcularPuntosAcumulados(Integer idCliente) {
        Integer puntos = puntosRepository.calcularPuntosAcumulados(idCliente, TipoTransaccion.GANADO);
        return puntos != null ? puntos : 0;
    }

    @Transactional
    public PuntosDTO generarPuntos(Integer idCliente, Integer idRegla, Integer cantidad) {
        ReglaPuntos regla = reglaPuntosRepository.findById(idRegla)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de puntos no encontrada con ID: " + idRegla));
        
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente));
        
        Puntos puntos = new Puntos();
        puntos.setCantPuntos(cantidad);
        puntos.setTipoTransaccion(TipoTransaccion.GANADO);
        puntos.setFechaTransaccion(LocalDate.now());
        puntos.setCliente(cliente);
        puntos.setReglaPuntos(regla);
        puntos.setActivo(true);
        
        if (regla.getTipoRegla() == TipoRegla.COMPRA) {
            // Los puntos por compra tienen vencimiento
            puntos.setFechaVencimiento(LocalDate.now().plusYears(1));
        }
        
        Puntos guardado = puntosRepository.save(puntos);
        log.info("Puntos generados: {} para cliente ID: {}", cantidad, idCliente);
        return new PuntosDTO(guardado);
    }

    // ============ CANJES ============
    
    public List<CanjeDTO> listarCanjesPorCliente(Integer idCliente) {
        return canjeRepository.findByOrdenCompra_Cliente_IdPersona(idCliente).stream()
                .map(CanjeDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public CanjeDTO realizarCanje(CanjeRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + request.getIdCliente()));
        
        OrdenCompra orden = ordenCompraRepositorio.findById(request.getIdOrdenCompra())
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + request.getIdOrdenCompra()));
        
        // Validar que la orden pertenezca al cliente
        if (!orden.getCliente().getIdPersona().equals(cliente.getIdPersona())) {
            throw new BusinessException("La orden no pertenece al cliente autenticado"); 
        }
        
        // Validar que la orden esté en estado PENDIENTE
        if (orden.getEstado() != pe.edu.pucp.fasticket.model.compra.EstadoCompra.PENDIENTE) {
            throw new BusinessException("Solo se pueden canjear puntos en órdenes pendientes");
        }
        
        // El monto a canjear debe ser el TOTAL de la orden (subtotal - descuento por membresía)
        // Primero calculamos el total que debería tener la orden sin canje
        orden.calcularTotal();
        Double totalOrden = orden.getTotal();
        
        // Obtener regla de canje activa
        List<ReglaPuntos> reglasCanje = reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.CANJE);
        if (reglasCanje.isEmpty()) {
            throw new BusinessException("No hay reglas de canje activas");
        }
        
        ReglaPuntos reglaCanje = reglasCanje.get(0);
        
        // Calcular puntos necesarios para cubrir el total de la orden
        Integer puntosNecesarios = (int) Math.ceil(totalOrden / reglaCanje.getSolesPorPunto());
        
        // Verificar que el cliente tenga suficientes puntos
        Integer puntosDisponibles = calcularPuntosAcumulados(request.getIdCliente());
        if (puntosDisponibles < puntosNecesarios) {
            throw new BusinessException("Puntos insuficientes. Disponibles: " + puntosDisponibles + ", Necesarios: " + puntosNecesarios);
        }
        
        // Validar que los puntos enviados coincidan con los necesarios
        if (!request.getPuntosCanje().equals(puntosNecesarios)) {
            throw new BusinessException("La cantidad de puntos debe ser " + puntosNecesarios + " para cubrir el total de la orden");
        }
        
        // El monto de descuento debe ser exactamente el total de la orden
        if (!request.getMontoDescuento().equals(totalOrden)) {
            throw new BusinessException("El monto de descuento debe ser el total de la orden: " + totalOrden);
        }
        
        // Crear registro de puntos perdidos (canje)
        Puntos puntosCanje = new Puntos();
        puntosCanje.setCantPuntos(request.getPuntosCanje());
        puntosCanje.setTipoTransaccion(TipoTransaccion.PERDIDO);
        puntosCanje.setFechaTransaccion(LocalDate.now());
        puntosCanje.setCliente(cliente);
        puntosCanje.setReglaPuntos(reglaCanje);
        puntosCanje.setActivo(true);
        
        Puntos puntosGuardados = puntosRepository.save(puntosCanje);
        
        // Aplicar el descuento por canje a la orden
        orden.setDescuentoPorCanje(request.getMontoDescuento());
        orden.aplicarDescuentoYRecalcular(); // Esto establecerá el total en 0
        ordenCompraRepositorio.save(orden);
        
        // Crear registro de canje
        Canje canje = new Canje();
        canje.setFechaCanje(LocalDate.now());
        canje.setOrdenCompra(orden);
        canje.setPuntos(puntosGuardados);
        
        Canje canjeGuardado = canjeRepository.save(canje);
        log.info("Canje realizado: {} puntos (soles {} por punto) para cubrir orden ID: {} con total: {}", 
                request.getPuntosCanje(), reglaCanje.getSolesPorPunto(), request.getIdOrdenCompra(), totalOrden);
        
        return new CanjeDTO(canjeGuardado);
    }

    // ============ CÓDIGOS PROMOCIONALES ============
    
    public List<CodigoPromocionalDTO> listarCodigosPromocionales() {
        return codigoPromocionalRepository.findAll().stream()
                .map(CodigoPromocionalDTO::new)
                .collect(Collectors.toList());
    }

    public CodigoPromocionalDTO obtenerCodigoPromocional(Integer id) {
        CodigoPromocional codigo = codigoPromocionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado con ID: " + id));
        return new CodigoPromocionalDTO(codigo);
    }

    public CodigoPromocionalDTO obtenerPorCodigo(String codigo) {
        CodigoPromocional codigoPromo = codigoPromocionalRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado: " + codigo));
        return new CodigoPromocionalDTO(codigoPromo);
    }

    @Transactional
    public CodigoPromocionalDTO crearCodigoPromocional(CodigoPromocionalRequestDTO request) {
        if (codigoPromocionalRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe un código promocional con el código: " + request.getCodigo());
        }
        
        CodigoPromocional codigo = new CodigoPromocional();
        codigo.setCodigo(request.getCodigo());
        codigo.setDescripcion(request.getDescripcion());
        codigo.setFechaFin(request.getFechaFin());
        codigo.setTipo(request.getTipo());
        codigo.setValor(request.getValor());
        codigo.setStock(request.getStock());
        codigo.setCantidadPorCliente(request.getCantidadPorCliente());
        
        CodigoPromocional guardado = codigoPromocionalRepository.save(codigo);
        log.info("Código promocional creado con ID: {}", guardado.getIdCodigoPromocional());
        return new CodigoPromocionalDTO(guardado);
    }

    @Transactional
    public CodigoPromocionalDTO actualizarCodigoPromocional(Integer id, CodigoPromocionalRequestDTO request) {
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
        
        CodigoPromocional actualizado = codigoPromocionalRepository.save(codigo);
        log.info("Código promocional actualizado con ID: {}", actualizado.getIdCodigoPromocional());
        return new CodigoPromocionalDTO(actualizado);
    }

    @Transactional
    public void eliminarCodigoPromocional(Integer id) {
        CodigoPromocional codigo = codigoPromocionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado con ID: " + id));
        
        codigoPromocionalRepository.delete(codigo);
        log.info("Código promocional eliminado con ID: {}", id);
    }

    // ============ MÉTODOS AUXILIARES ============
    
    @Transactional
    public Double calcularDescuentoPorMembresia(TipoMembresia tipoMembresia, Integer cantidadEntradas) {
        double porcentajeDescuento = 0.0;
        
        switch (tipoMembresia) {
            case BRONCE, PLATA, ORO -> {
                if (cantidadEntradas < 10) {
                    porcentajeDescuento = 0.02; // 2%
                } else if (cantidadEntradas < 50) {
                    porcentajeDescuento = 0.05; // 5%
                } else {
                    porcentajeDescuento = 0.10; // 10%
                }
            }
        }
        
        return porcentajeDescuento;
    }

    @Transactional
    public void generarPuntosPorCompra(Integer idCliente, Double montoTotal, Integer idOrdenCompra) {
        List<ReglaPuntos> reglasCompra = reglaPuntosRepository.findByTipoReglaAndActivoTrue(TipoRegla.COMPRA);
        
        if (reglasCompra.isEmpty()) {
            log.warn("No hay reglas de compra activas para generar puntos");
            return;
        }
        
        ReglaPuntos reglaCompra = reglasCompra.get(0);
        
        // Calcular puntos basado en solesPorPunto
        Integer puntosGenerados = (int) (montoTotal / reglaCompra.getSolesPorPunto());
        
        if (puntosGenerados > 0) {
            generarPuntos(idCliente, reglaCompra.getIdRegla(), puntosGenerados);
            log.info("Generados {} puntos por compra ID: {} para cliente ID: {}", puntosGenerados, idOrdenCompra, idCliente);
        }
    }

    @Transactional
    public void aplicarDescuentoPorCodigoPromocional(Integer idOrdenCompra, String codigo) {
        CodigoPromocional codigoPromo = codigoPromocionalRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado: " + codigo));
        
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
}

