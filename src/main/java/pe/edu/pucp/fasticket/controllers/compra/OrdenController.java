package pe.edu.pucp.fasticket.controllers.compra;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.compra.CheckoutCarritoRequestDTO;
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.eventos.EventoResumenDTO;
import pe.edu.pucp.fasticket.dto.fidelizacion.ValidacionCuponResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.repository.pago.ComprobanteDePagoRepositorio;
import pe.edu.pucp.fasticket.security.UserDetailsImpl;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;
import pe.edu.pucp.fasticket.services.S3Service;

@Tag(
        name = "Órdenes de Compra",
        description = "API para la gestión de órdenes de compra. " +
                      "El cliente se identifica automáticamente mediante el token JWT. " +
                      "Los datos de asistentes no se requieren en la creación de órdenes."
)
@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Slf4j
public class OrdenController {

    private final OrdenServicio ordenServicio;
    private final OrdenCompraRepositorio ordenCompraRepositorio;
    private final TipoTicketRepositorio tipoTicketRepositorio;
    private final FidelizacionService fidelizacionService;
    private final ComprobanteDePagoRepositorio comprobanteDePagoRepositorio;
    private final S3Service s3Service;

    @Operation(
            summary = "Crear nueva orden ",
            description = "Crea una orden PENDIENTE y reserva tickets para el cliente autenticado. " +
                          "El ID del cliente se obtiene automáticamente del token JWT, no es necesario enviarlo en el cuerpo de la petición. " +
                          "Los datos de asistentes NO se requieren en la creación de la orden. " +
                          "La orden se crea con estado PENDIENTE y tiene un tiempo de expiración de 15 minutos. " +
                          "Requiere autenticación con rol CLIENTE.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Orden creada exitosamente. Los tickets han sido reservados y están en estado RESERVADA.",
                    content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos, stock insuficiente, límite de tickets por compra excedido, o límite por persona excedido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Se requiere un token JWT válido en el header Authorization."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos. Se requiere rol CLIENTE."
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> crearOrden(
            @Valid @RequestBody CrearOrdenDTO crearOrdenDTO,
            Authentication authentication) {

        boolean esCliente = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        if (!esCliente) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(StandardResponse.error("No tiene permisos para acceder a este recurso", null));
        }

        Integer idCliente = crearOrdenDTO.getIdCliente() != null
                ? crearOrdenDTO.getIdCliente()
                : obtenerIdUsuarioLogueado(authentication);

        log.info("POST /api/v1/ordenes - Cliente: {}", idCliente);

        // Crear la orden
        OrdenCompra nuevaOrden = ordenServicio.crearOrden(crearOrdenDTO, idCliente);

        // IMPORTANTE: Recargar la orden con todas las relaciones para el DTO
        OrdenCompra ordenCompleta = ordenCompraRepositorio.findByIdWithAllDetails(nuevaOrden.getIdOrdenCompra())
                .orElseThrow(() -> new ResourceNotFoundException("Error al cargar la orden creada"));

        // Crear el DTO con la orden completa
        OrdenResumenDTO resumenDTO = new OrdenResumenDTO(ordenCompleta, tipoTicketRepositorio);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StandardResponse.success("Orden creada exitosamente.", resumenDTO));
    }

    @Operation(
            summary = "Obtener resumen de una orden creada",
            description = "Obtiene los detalles completos de una orden existente, incluyendo items, descuentos, IGV y totales. Clientes solo pueden ver sus propias órdenes, administradores pueden ver cualquier orden.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Resumen obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Orden no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> obtenerDetalleDeOrden(
            @PathVariable Integer id) {

        log.info("GET /api/v1/ordenes/{}", id);OrdenCompra orden = ordenCompraRepositorio.findByIdWithAllDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));

        OrdenResumenDTO resumen = new OrdenResumenDTO(orden, tipoTicketRepositorio);
        return ResponseEntity.ok(StandardResponse.success("Proceso iniciado correctamente.", resumen));
    }

    @Operation(
            summary = "Cancelar una orden de compra",
            description = "Permite cancelar una orden PENDIENTE antes del pago. Revierte los tickets a estado DISPONIBLE y libera el stock. Solo se pueden cancelar órdenes en estado PENDIENTE. Requiere rol CLIENTE o ADMINISTRADOR.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Orden cancelada correctamente"
            ),
            @ApiResponse(
                    responseCode = "404", description = "Orden no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "No se puede cancelar una orden en este estado. Solo se pueden cancelar órdenes PENDIENTES.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> cancelarOrden(
            @Parameter(description = "ID de la orden a cancelar", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("PUT /api/v1/ordenes/{}/cancelar", id);

        ordenServicio.cancelarOrden(id);
        return ResponseEntity.ok(
                StandardResponse.success("Orden cancelada correctamente.", null)
        );
    }
    @Operation(
            summary = "Confirmar pago de una orden",
            description = "Confirma una orden tras un pago exitoso. Actualiza los tickets a estado VENDIDA, genera puntos de fidelización, envía correo de confirmación y actualiza el aforo del evento. Solo se pueden confirmar órdenes en estado PENDIENTE. Requiere rol CLIENTE o ADMINISTRADOR.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Orden confirmada correctamente"
            ),
            @ApiResponse(
                    responseCode = "404", description = "Orden no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "No se puede confirmar una orden en este estado. Solo se pueden confirmar órdenes PENDIENTES.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> confirmarOrden(
            @Parameter(description = "ID de la orden a confirmar", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("PUT /api/v1/ordenes/{}/confirmar", id);
        ordenServicio.confirmarPagoOrden(id);
        return ResponseEntity.ok(StandardResponse.success("Orden confirmada correctamente.", null));
    }

    @Operation(
            summary = "Listar órdenes de compra",
            description = "Lista órdenes según el rol del usuario. Clientes ven solo sus órdenes, administradores ven todas. Permite filtrar por estado.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<OrdenResumenDTO>>> listarOrdenes(
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(required = false) Integer idCliente,
            Authentication authentication) {

        log.info("GET /api/v1/ordenes - Estado: {}, Cliente: {}", estado, idCliente);

        List<OrdenCompra> ordenes;
        Integer idUsuarioAutenticado = obtenerIdUsuarioLogueado(authentication);

        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))) {
            if (idCliente != null) {
                if (estado != null) {
                    ordenes = ordenServicio.listarOrdenesPorClienteYEstado(idCliente, estado);
                } else {
                    ordenes = ordenServicio.listarOrdenesPorCliente(idCliente);
                }
            } else if (estado != null) {
                ordenes = ordenServicio.listarOrdenesPorEstado(estado);
            } else {
                ordenes = ordenServicio.listarTodasLasOrdenes();
            }
        } else {
            if (estado != null) {
                ordenes = ordenServicio.listarOrdenesPorClienteYEstado(idUsuarioAutenticado, estado);
            } else {
                ordenes = ordenServicio.listarOrdenesPorCliente(idUsuarioAutenticado);
            }
        }

        // Recargar cada orden con sus relaciones para el DTO
        List<OrdenResumenDTO> resumenes = ordenes.stream()
                .map(orden -> {
                    OrdenCompra ordenCompleta = ordenCompraRepositorio.findByIdWithAllDetails(orden.getIdOrdenCompra())
                            .orElse(orden); // Fallback a la orden original si falla
                    return new OrdenResumenDTO(ordenCompleta, tipoTicketRepositorio);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(StandardResponse.success("Órdenes obtenidas exitosamente.", resumenes));
    }

    @Operation(
            summary = "Anular una orden de compra (Administrador)",
            description = "Permite a un administrador anular una orden APROBADA. Revierte el stock y registra auditoría. Requiere rol ADMINISTRADOR.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden anulada correctamente"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "No se puede anular una orden en este estado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PutMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> anularOrden(
            @Parameter(description = "ID de la orden a anular", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("PUT /api/v1/ordenes/{}/anular", id);
        ordenServicio.anularCompraAdmin(id);
        return ResponseEntity.ok(StandardResponse.success("Orden anulada correctamente.", null));
    }

    /**
     * Obtiene el ID del usuario autenticado desde el contexto de seguridad.
     */
    private Integer obtenerIdUsuarioLogueado(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new SecurityException("No se pudo determinar el usuario autenticado.");
        }
        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getIdPersona();
        }
        throw new SecurityException("El principal de autenticación no es del tipo esperado.");
    }

    @Operation(summary = "Descargar Comprobante PDF", description = "Descarga el PDF generado al momento de la compra.")
    @GetMapping("/{idOrden}/comprobante")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarComprobante(
            @PathVariable Integer idOrden,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 1. Buscamos la orden
        OrdenCompra orden = ordenCompraRepositorio.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        // 2. Seguridad: Verificar que sea el dueño o un admin
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        if (!isAdmin && !orden.getCliente().getEmail().equals(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // 3. Verificar que la orden tenga pago
        if (orden.getPago() == null || orden.getPago().getComprobantePago() == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 4. Cargar directamente el comprobante desde el repositorio para evitar problemas con relaciones lazy
        pe.edu.pucp.fasticket.model.pago.ComprobantePago comprobante = comprobanteDePagoRepositorio
                .findById(orden.getPago().getComprobantePago().getIdComprobante())
                .orElse(null);
        
        // 5. Verificar que el comprobante tenga URL del PDF en S3
        if (comprobante == null || comprobante.getPdfUrl() == null || comprobante.getPdfUrl().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 6. Descargar el PDF desde S3
        try {
            byte[] pdfContent = s3Service.downloadFile(comprobante.getPdfUrl());
            
            if (pdfContent == null || pdfContent.length == 0) {
                log.error("PDF vacío descargado desde S3 para orden {}", idOrden);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Validar que el PDF sea válido (debe empezar con %PDF)
            if (pdfContent.length < 4) {
                log.error("PDF corrupto para orden {}: tamaño insuficiente ({} bytes)", idOrden, pdfContent.length);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            String header = new String(pdfContent, 0, Math.min(4, pdfContent.length));
            if (!header.equals("%PDF")) {
                log.error("PDF corrupto para orden {}: no tiene header PDF válido (inicio: {})", idOrden, header);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // 7. Retornar bytes del PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "comprobante-ORD-" + idOrden + ".pdf");
            headers.setContentLength(pdfContent.length);

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error al descargar PDF desde S3 para orden {}: {}", idOrden, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Asignar Asistentes y Crear Orden desde Carrito",
            description = "Crea una orden desde un carrito existente asignando asistentes a cada ticket. Requiere rol CLIENTE.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o carrito vacío", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PostMapping("/checkout-carrito/{idCarrito}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> checkoutDesdeCarrito(
            @Parameter(description = "ID del carrito de compras", required = true, example = "1")
            @PathVariable Integer idCarrito,
            @Valid @RequestBody CheckoutCarritoRequestDTO request) {
        OrdenCompra orden = ordenServicio.checkoutDesdeCarrito(idCarrito, request);
        log.info("POST /api/v1/ordenes/checkout-carrito/{} - Orden ID: {}", idCarrito, orden.getIdOrdenCompra());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success("Orden creada, pendiente de pago.", new OrdenResumenDTO(orden, tipoTicketRepositorio)));
    }

    @Operation(
            summary = "Validar cupón ",
            description = "Verifica si un código es válido y retorna el monto de descuento calculado, sin aplicarlo ni restar stock."
    )
    @GetMapping("/validar-cupon")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<ValidacionCuponResponseDTO>> validarCupon(
            @RequestParam String codigo,
            @Parameter(hidden = true) Authentication authentication) {

        try {
            Integer idCliente = obtenerIdUsuarioLogueado(authentication);

            // Llamamos al servicio sin el monto
            ValidacionCuponResponseDTO infoCupon = fidelizacionService.validarCodigoPromocional(codigo, idCliente);

            return ResponseEntity.ok(StandardResponse.success(
                    "Cupón válido",
                    infoCupon
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new StandardResponse<>(false, e.getMessage(), null)
            );
        }
    }
    @Operation(
            summary = "Obtener info del evento por Tipo de Ticket (Pre-compra)",
            description = "Devuelve los datos del evento basándose en el ID del tipo de ticket. Útil para mostrar el resumen en compra directa antes de crear la orden.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/evento-info-por-ticket")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<EventoResumenDTO>> obtenerInfoPorTicket(
            @RequestParam Integer idTipoTicket) {

        EventoResumenDTO info = ordenServicio.obtenerEventoPorTipoTicket(idTipoTicket);

        return ResponseEntity.ok(StandardResponse.success(
                "Información del evento recuperada",
                info
        ));
    }
}