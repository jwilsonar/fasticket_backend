package pe.edu.pucp.fasticket.services.usuario;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.compra.EventoHistorialDTO;
import pe.edu.pucp.fasticket.dto.compra.HistorialCompraDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemHistorialDTO;
import pe.edu.pucp.fasticket.dto.compra.PagoHistorialDTO;
import pe.edu.pucp.fasticket.dto.compra.TicketHistorialDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.dto.tickets.MisEntradasDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilEditDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilResponseDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilUpdateDTO;
import pe.edu.pucp.fasticket.exception.BusinessException;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.mapper.EventoMapper;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.geografia.Distrito;
import pe.edu.pucp.fasticket.model.usuario.Administrador;
import pe.edu.pucp.fasticket.model.usuario.Cliente;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.EventosRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TicketRepository;
import pe.edu.pucp.fasticket.repository.geografia.DistritoRepository;
import pe.edu.pucp.fasticket.repository.usuario.AdministradorRepository;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.repository.usuario.PersonasRepositorio;
import pe.edu.pucp.fasticket.services.auditoria.AuditLogService;

/**
 * Servicio para gestión de clientes.
 * Implementa RF-030, RF-032, RF-060, RF-091.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PersonasRepositorio personasRepositorio;
    private final DistritoRepository distritoRepositorio;
    private final EventosRepositorio eventoRepositorio;
    private final EventoMapper eventoMapper;
    private final AuditLogService auditLogService;
    private final AdministradorRepository administradorRepository;
    private final TicketRepository ticketRepositorio;
    private final OrdenCompraRepositorio ordenCompraRepositorio;
    /**
     * RF-030: Obtiene el perfil del cliente por email.
     * 
     * @param email Email del cliente
     * @return Perfil del cliente
     */
    public ClientePerfilResponseDTO obtenerPerfilPorEmail(String email) {
        log.info("Obteniendo perfil del cliente con email: {}", email);
        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));
        return convertirAPerfilDTO(cliente);
    }


    /**
     * RF-030: Obtiene el perfil del cliente por ID.
     * 
     * @param id ID del cliente
     * @return Perfil del cliente
     */
    public ClientePerfilResponseDTO obtenerPerfilPorId(Integer id) {
        log.info("Obteniendo perfil del cliente con ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return convertirAPerfilDTO(cliente);
    }

    /**
     * RF-060: Actualiza el perfil del cliente.
     * 
     * @param email Email del cliente autenticado
     * @param dto Datos a actualizar
     * @return Perfil actualizado
     */
    @Transactional
    public ClientePerfilResponseDTO actualizarPerfil(String email, ClientePerfilUpdateDTO dto) {
        log.info("Actualizando perfil del cliente: {}", email);
        
        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));

        // Actualizar campos si vienen en el DTO
        if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
            cliente.setNombres(dto.getNombres());
        }
        if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
            cliente.setApellidos(dto.getApellidos());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            cliente.setTelefono(dto.getTelefono());
        }
        if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {
            cliente.setDireccion(dto.getDireccion());
        }
        
        // Validar que el nuevo email no esté en uso por otro cliente
        if (dto.getEmail() != null && !dto.getEmail().equals(cliente.getEmail())) {
            if (personasRepositorio.existsByEmail(dto.getEmail())) {
                throw new BusinessException("El email ya está registrado por otro usuario");
            }
            cliente.setEmail(dto.getEmail());
        }

        cliente.setFechaActualizacion(java.time.LocalDate.now());
        Cliente clienteActualizado = clienteRepository.save(cliente);

        log.info("Perfil actualizado exitosamente para: {}", email);
        return convertirAPerfilDTO(clienteActualizado);
    }

    /**
     * RF-060: Actualiza el perfil del cliente(pero para el Administrador).
     *
     * @param id ID del cliente autenticado
     * @param dto Datos a actualizar
     * @return Perfil actualizado
     */
    @Transactional
    public ClientePerfilResponseDTO editarPerfil(Integer id, ClientePerfilEditDTO dto) {
        log.info("Actualizando perfil del cliente de ID: {}", id);
        Administrador adminActual = getAdminActual(); // Obtener admin

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        // Actualizar campos si vienen en el DTO
        if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
            cliente.setNombres(dto.getNombres());
        }
        if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
            cliente.setApellidos(dto.getApellidos());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            cliente.setTelefono(dto.getTelefono());
        }
        if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {
            cliente.setDireccion(dto.getDireccion());
        }
        if (dto.getDocIdentidad() != null && !dto.getDocIdentidad().isBlank()) {
            cliente.setDocIdentidad(dto.getDocIdentidad());
        }

        if (dto.getIdDistrito() != null){
            Distrito distrito = distritoRepositorio.findById(dto.getIdDistrito())
                    .orElseThrow(() -> new ResourceNotFoundException("Distrito no encontrado con ID: " + dto.getIdDistrito()));
            cliente.setDistrito(distrito);
        }

        // Validar que el nuevo email no esté en uso por otro cliente
        if (dto.getEmail() != null && !dto.getEmail().equals(cliente.getEmail())) {
            if (personasRepositorio.existsByEmail(dto.getEmail())) {
                throw new BusinessException("El email ya está registrado por otro usuario");
            }
            cliente.setEmail(dto.getEmail());
        }

        cliente.setFechaActualizacion(java.time.LocalDate.now());
        Cliente clienteActualizado = clienteRepository.save(cliente);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") editó el perfil del Cliente: " + cliente.getEmail() + " (ID: " + id + ")";
            auditLogService.registrarAuditoria(adminActual, "EDITAR_PERFIL_CLIENTE", "ClienteService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (EDITAR_PERFIL_CLIENTE): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Perfil actualizado exitosamente para: {}", id);
        return convertirAPerfilDTO(clienteActualizado);
    }

    /**
     * RF-032, RF-091: Obtiene el historial de compras del cliente por email.
     * 
     * @param email Email del cliente
     * @return Lista de DTOs de historial de compras
     */
    public List<HistorialCompraDTO> obtenerHistorialCompras(String email) {
        log.info("Obteniendo historial de compras para: {}", email);
        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));
        
        List<OrdenCompra> ordenes = ordenCompraRepositorio.findByClienteIdWithAllDetails(cliente.getIdPersona());
        return ordenes.stream()
                .map(this::convertirAHistorialDTO)
                .collect(Collectors.toList());
    }

    /**
     * RF-032, RF-091: Obtiene el historial de compras del cliente por ID.
     * 
     * @param id ID del cliente
     * @return Lista de DTOs de historial de compras
     */
    public List<HistorialCompraDTO> obtenerHistorialComprasPorId(Integer id) {
        log.info("Obteniendo historial de compras para cliente ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        
        List<OrdenCompra> ordenes = ordenCompraRepositorio.findByClienteIdWithAllDetails(cliente.getIdPersona());
        return ordenes.stream()
                .map(this::convertirAHistorialDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una compra individual por ID de orden.
     * 
     * @param idOrdenCompra ID de la orden de compra
     * @param emailCliente Email del cliente (para validar que sea su orden)
     * @return DTO de historial de compra
     */
    public HistorialCompraDTO obtenerCompraIndividual(Integer idOrdenCompra, String emailCliente) {
        log.info("Obteniendo compra individual ID: {} para cliente: {}", idOrdenCompra, emailCliente);
        
        OrdenCompra orden = ordenCompraRepositorio.findByIdWithAllDetailsForHistorial(idOrdenCompra)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + idOrdenCompra));
        
        // Validar que la orden pertenezca al cliente
        if (!orden.getCliente().getEmail().equals(emailCliente)) {
            throw new BusinessException("No tiene permisos para acceder a esta orden");
        }
        
        return convertirAHistorialDTO(orden);
    }

    /**
     * Convierte una OrdenCompra a HistorialCompraDTO.
     * 
     * @param orden Orden de compra
     * @return DTO de historial
     */
    private HistorialCompraDTO convertirAHistorialDTO(OrdenCompra orden) {
        HistorialCompraDTO dto = new HistorialCompraDTO();

        // 1. DATOS GENERALES
        dto.setIdOrden(orden.getIdOrdenCompra());
        dto.setCodigoCompra("ORD-" + orden.getIdOrdenCompra());
        dto.setEstado(orden.getEstado().toString());
        // Usamos fechaCreacion para la fecha de compra
        dto.setFechaCompra(orden.getFechaCreacion() != null ? orden.getFechaCreacion().atStartOfDay() : orden.getFechaOrden().atStartOfDay());
        dto.setCodigoSeguimiento(orden.getCodigoSeguimiento());

        // 2. DATOS DEL EVENTO (Tomados del primer ticket)
        if (orden.getItems() != null && !orden.getItems().isEmpty()) {
            var tipoTicket = orden.getItems().get(0).getTipoTicket();
            var evento = tipoTicket.getEvento();

            dto.setNombreEvento(evento.getNombre());
            dto.setLugarEvento(evento.getLocal().getNombre());
            // Opcional: Dirección
            dto.setDireccionLocal(evento.getLocal().getDireccion());

            // Fecha del evento (para mostrar cuándo es el concierto)
            dto.setFechaEvento(evento.getFechaEvento().atStartOfDay());
            dto.setImagenUrl(evento.getImagenUrl());
        }

        // 3. DATOS FINANCIEROS (Desglose)
        dto.setSubtotal(orden.getSubtotal());

        // Descuentos (Manejando nulos)
        dto.setDescuentoCupon(orden.getDescuentoPromocional() != null ? orden.getDescuentoPromocional() : 0.0);
        dto.setDescuentoPuntos(orden.getDescuentoPorCanje() != null ? orden.getDescuentoPorCanje() : 0.0);

        dto.setTotalPagado(orden.getTotal());

        // 4. FIDELIZACIÓN (Puntos)
        // Puntos Ganados (Cálculo estimado o real si lo guardaste)
        // Regla: 1 punto por sol (ejemplo). Ajusta según tu regla real.
        dto.setPuntosGanados(orden.getPuntosGanados() != null ? orden.getPuntosGanados() : 0);

        // Puntos Canjeados (Estimación inversa si hubo descuento por canje)
        // Ej: Si descontó 20 soles y la regla es 10 pts/sol -> usó 200 puntos.
        if (dto.getDescuentoPuntos() > 0) {
            // Ajusta el '10' por tu factor de canje real o lee de config
            dto.setPuntosCanjeados((int) (dto.getDescuentoPuntos() * 10));
        } else {
            dto.setPuntosCanjeados(0);
        }

        // 5. DATOS DEL PAGO
        if (orden.getPago() != null) {
            var pago = orden.getPago();
            dto.setIdTransaccionPago(pago.getIdPago());
            dto.setMedioPago(pago.getMetodo()); // Ej: "Tarjeta (4242)"
            dto.setEstadoPago(pago.getEstado().toString());

            // Enmascarar tarjeta visualmente si viene en el texto del método
            if (pago.getMetodo().contains("(")) {
                String last4 = pago.getMetodo().substring(pago.getMetodo().indexOf("(") + 1, pago.getMetodo().indexOf(")"));
                dto.setNumeroTarjeta("**** **** **** " + last4);
            } else {
                dto.setNumeroTarjeta("**** **** **** ****");
            }

            // Verificar si hay comprobante para mostrar botón "Descargar"
            dto.setTieneComprobante(pago.getComprobantePago() != null);
        } else {
            dto.setTieneComprobante(false);
            dto.setMedioPago("Pendiente");
            dto.setNumeroTarjeta("---");
        }

        // 6. ITEMS Y ASISTENTES (El detalle más importante)
        List<HistorialCompraDTO.DetalleItemDTO> itemsDTO = orden.getItems().stream().map(item -> {
            // Mapear cada ticket individual como un asistente
            List<HistorialCompraDTO.DetalleAsistenteDTO> asistentes = item.getTickets().stream().map(t -> {
                return HistorialCompraDTO.DetalleAsistenteDTO.builder()
                        .idTicket(t.getIdTicket())
                        .nombreCompleto(t.getNombreAsistente() + " " + t.getApellidoAsistente())
                        .documento((t.getTipoDocumentoAsistente() != null ? t.getTipoDocumentoAsistente() : "DOC") + ": " + t.getDocumentoAsistente())
                        .codigoQr(t.getCodigoQr()) // El texto del QR para generarlo en el front si se quiere
                        .build();
            }).collect(Collectors.toList());

            return HistorialCompraDTO.DetalleItemDTO.builder()
                    .nombreTipoTicket(item.getTipoTicket().getNombre()) // "V.I.P"
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioFinal()) // Precio real pagado por ticket
                    .subtotalLinea(item.getPrecioFinal() * item.getCantidad())
                    .asistentes(asistentes)
                    .build();
        }).collect(Collectors.toList());

        dto.setItems(itemsDTO);

        return dto;
    }

    /**
     * RF-030: Obtiene los perfiles de clientes por Nivel.
     * 
     * @param nivel Nivel del cliente
     * @return Lista de perfiles de clientes
     */
    public List<ClientePerfilResponseDTO> obtenerPerfilesPorNivel(TipoMembresia nivel) {
        log.info("Obteniendo perfiles de clientes con nivel: {}", nivel);
        List<Cliente> clientes = clienteRepository.findByNivel(nivel);
        return (clientes == null)
            ? Collections.emptyList()
            : clientes.stream()
                    .map(this::convertirAPerfilDTO)
                    .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista de todos los cliente
     * */

    public List<ClientePerfilResponseDTO> listarTodos() {
        log.info("Obteniendo perfiles de todos los clientes");
        List<Cliente> clientes = clienteRepository.findAll();
        return clientes.stream().map(this::convertirAPerfilDTO).collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Cliente a ClientePerfilDTO.
     * 
     * @param cliente Entidad cliente
     * @return DTO con información del perfil
     */
    private ClientePerfilResponseDTO convertirAPerfilDTO(Cliente cliente) {
        ClientePerfilResponseDTO dto = new ClientePerfilResponseDTO();
        dto.setIdCliente(cliente.getIdPersona());
        dto.setTipoDocumento(cliente.getTipoDocumento());
        dto.setDocIdentidad(cliente.getDocIdentidad());
        dto.setNombres(cliente.getNombres());
        dto.setApellidos(cliente.getApellidos());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setFechaNacimiento(cliente.getFechaNacimiento());
        dto.setDireccion(cliente.getDireccion());
        dto.setPuntosAcumulados(cliente.getPuntosAcumulados());
        dto.setNivel(cliente.getNivel());
        dto.setEdad(cliente.calcularEdad());
        dto.setFechaCreacion(cliente.getFechaCreacion());
        dto.setVerificado(cliente.getVerificado());
        dto.setActivo(cliente.getActivo());
        return dto;
    }

    @Transactional
    public void desactivarCliente(Integer idCliente) {
        log.warn("Solicitud de desactivación (borrado lógico) para cliente ID: {}", idCliente);
        Administrador adminActual = getAdminActual(); // Obtener admin

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente));
        if (!cliente.getActivo()) {
            throw new BusinessException("El cliente ya se encuentra desactivado.");
        }
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") desactivó la cuenta del Cliente: " + cliente.getEmail() + " (ID: " + idCliente + ")";
            auditLogService.registrarAuditoria(adminActual, "DESACTIVAR_CLIENTE", "ClienteService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (DESACTIVAR_CLIENTE): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Cliente ID: {} desactivado exitosamente.", idCliente);
    }

    /**
     * Permite al cliente autenticado desactivar su propia cuenta (borrado lógico).
     * El cliente no será eliminado físicamente, solo se marcará como inactivo.
     * 
     * @param email Email del cliente autenticado
     * @throws ResourceNotFoundException si el cliente no existe
     * @throws BusinessException si el cliente ya está desactivado
     */
    @Transactional
    public void desactivarMiCuenta(String email) {
        log.warn("Solicitud de auto-desactivación (borrado lógico) para cliente con email: {}", email);

        Cliente cliente = (Cliente) personasRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + email));
        
        if (!cliente.getActivo()) {
            throw new BusinessException("Su cuenta ya se encuentra desactivada.");
        }
        
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        log.info("Cliente con email: {} ha desactivado su cuenta exitosamente.", email);
    }

    /**
     * NUEVO MÉTODO PARA RF-031: Marcar cliente como verificado
     * Permite a un admin marcar el correo/teléfono de un cliente como verificado.
     */
    @Transactional
    public ClientePerfilResponseDTO marcarComoVerificado(Integer idCliente) {
        log.info("Solicitud de verificación para cliente ID: {}", idCliente);
        Administrador adminActual = getAdminActual(); // Obtener admin

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente));

        if (Boolean.TRUE.equals(cliente.getVerificado())) {
            throw new BusinessException("El cliente ya se encuentra verificado.");
        }

        // Asumimos que tienes un campo 'verificado' en tu entidad Cliente
        // Si no lo tienes, debes agregarlo: private Boolean verificado;
        cliente.setVerificado(true);
        Cliente clienteVerificado = clienteRepository.save(cliente);

        // --- INICIO AUDITORÍA RF-109 ---
        try {
            String detalle = "Admin (ID: " + adminActual.getIdPersona() + ") marcó como VERIFICADO al Cliente: " + cliente.getEmail() + " (ID: " + idCliente + ")";
            auditLogService.registrarAuditoria(adminActual, "VERIFICAR_CLIENTE", "ClienteService", detalle);
        } catch (Exception e) {
            log.error("Fallo al registrar auditoría (VERIFICAR_CLIENTE): {}", e.getMessage());
        }
        // --- FIN AUDITORÍA ---

        log.info("Cliente ID: {} marcado como verificado exitosamente.", idCliente);
        return convertirAPerfilDTO(clienteVerificado);
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

    /**
     * RF-074: Añade un evento a la lista de favoritos del cliente.
     */
    @Transactional
    public void agregarFavorito(String emailCliente, Integer idEvento) {
        log.info("Agregando evento ID: {} a favoritos de cliente: {}", idEvento, emailCliente);

        Cliente cliente = (Cliente) personasRepositorio.findByEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + emailCliente));

        Evento evento = eventoRepositorio.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        // Validación para evitar duplicados
        if (cliente.getEventosFavoritos().contains(evento)) {
            throw new BusinessException("El evento ya está en la lista de favoritos.");
        }

        cliente.getEventosFavoritos().add(evento);
        clienteRepository.save(cliente);
    }

    /**
     * RF-074: Quita un evento de la lista de favoritos del cliente.
     */
    @Transactional
    public void quitarFavorito(String emailCliente, Integer idEvento) {
        log.info("Quitando evento ID: {} de favoritos de cliente: {}", idEvento, emailCliente);

        Cliente cliente = (Cliente) personasRepositorio.findByEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + emailCliente));

        Evento evento = eventoRepositorio.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + idEvento));

        if (!cliente.getEventosFavoritos().remove(evento)) {
            throw new BusinessException("El evento no estaba en la lista de favoritos.");
        }

        clienteRepository.save(cliente);
    }

    /**
     * RF-074: Lista todos los eventos favoritos de un cliente.
     */
    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarFavoritos(String emailCliente) {
        log.info("Listando favoritos de cliente: {}", emailCliente);

        Cliente cliente = (Cliente) personasRepositorio.findByEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + emailCliente));

        // Usamos el EventoMapper para convertir la lista de Entidades a DTOs
        return cliente.getEventosFavoritos().stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarCuentaPropia(String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        if (!cliente.getActivo()) {
            throw new BusinessException("La cuenta ya está desactivada");
        }

        // Borrado lógico
        cliente.setActivo(false);

        // Anonimizar email (evitar colisiones)
        String anonId = (cliente.getIdPersona() != null) ? cliente.getIdPersona().toString() : String.valueOf(System.currentTimeMillis());
        String anonEmail = "deleted+" + anonId + "@deleted.fasticket";
        cliente.setEmail(anonEmail);

        // Anonimizar otros campos
        cliente.setNombres("ANONIMO");
        cliente.setApellidos("");
        try { cliente.setTelefono(null); } catch (Exception ignored) {}
        try { cliente.setDireccion(null); } catch (Exception ignored) {}
        try { cliente.setDocIdentidad(null); } catch (Exception ignored) {}

        clienteRepository.save(cliente);
        log.info("Cuenta del cliente con email original {} desactivada y anonimizada (id={})", email, anonId);
    }

    @Transactional(readOnly = true)
    public List<MisEntradasDTO> listarTicketsTransferibles(String emailCliente) {
        log.info("Listando tickets transferibles (vigentes y VENDIDA) para: {}", emailCliente);

        Cliente cliente = (Cliente) personasRepositorio.findByEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + emailCliente));

        List<Ticket> tickets = ticketRepositorio.findTicketsTransferiblesByCliente(
                cliente.getIdPersona(),
                LocalDate.now()
        );

        return tickets.stream()
                .map(MisEntradasDTO::new)
                .collect(Collectors.toList());
    }
}

