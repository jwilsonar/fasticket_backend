package pe.edu.pucp.fasticket.controllers.compra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.dto.compra.RegistrarParticipantesDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.repository.compra.OrdenCompraRepositorio;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

@Tag(
        name = "Órdenes de Compra",
        description = "API para la creación y consulta de órdenes de compra."
)
@RestController
@RequestMapping("/api/v1/ordenes")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class OrdenController {

    private final OrdenServicio ordenServicio;
    private final OrdenCompraRepositorio ordenCompraRepositorio;
    private final TipoTicketRepositorio tipoTicketRepositorio;

    @Operation(
            summary = "Crear nueva orden (Checkout directo)",
            description = "Crea una orden PENDIENTE y reserva tickets. Requiere rol CLIENTE.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> crearOrden(@Valid @RequestBody CrearOrdenDTO crearOrdenDTO) {
        log.info("POST /api/v1/ordenes - Cliente: {}", crearOrdenDTO.getIdCliente());
        OrdenCompra nuevaOrden = ordenServicio.crearOrden(crearOrdenDTO);
        OrdenResumenDTO resumenDTO = new OrdenResumenDTO(nuevaOrden, tipoTicketRepositorio);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StandardResponse.success("Proceso iniciado correctamente.", resumenDTO));
    }

    @Operation(
            summary = "Obtener resumen de una orden creada",
            description = "Obtiene los detalles de una orden ya existente.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen obtenido", content = @Content(schema = @Schema(implementation = OrdenResumenDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> obtenerDetalleDeOrden(
            @Parameter(description = "ID de la orden", required = true, example = "1")
            @PathVariable Integer id) {

        log.info("GET /api/v1/ordenes/{}", id);
        OrdenCompra orden = ordenCompraRepositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + id));
        OrdenResumenDTO resumen = new OrdenResumenDTO(orden, tipoTicketRepositorio);
        return ResponseEntity.ok(StandardResponse.success("Proceso iniciado correctamente.", resumen));
    }

    @Operation(
            summary = "Cancelar una orden de compra",
            description = "Permite cancelar una orden PENDIENTE antes del pago. Actualiza los tickets a estado DISPONIBLE.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden cancelada correctamente"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar una orden en este estado")
    })
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> cancelarOrden(
            @Parameter(description = "ID de la orden a cancelar", required = true)
            @PathVariable Integer id) {

        log.info("PUT /api/v1/ordenes/{}/cancelar", id);

        ordenServicio.cancelarOrden(id);
        return ResponseEntity.ok(
                StandardResponse.success("Orden cancelada correctamente.", null)
        );
    }
    @Operation(
            summary = "Confirmar pago de una orden",
            description = "Confirma una orden tras un pago exitoso. Actualiza los tickets a estado VENDIDA.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden confirmada correctamente"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "No se puede confirmar una orden en este estado")
    })
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> confirmarOrden(
            @Parameter(description = "ID de la orden a confirmar", required = true)
            @PathVariable Integer id) {

        log.info("PUT /api/v1/ordenes/{}/confirmar", id);
        ordenServicio.confirmarPagoOrden(id);
        return ResponseEntity.ok(StandardResponse.success("Orden confirmada correctamente.", null));
    }

    @Operation(
            summary = "Comprar el carrito de un cliente",
            description = "Convierte los ítems del carrito en una nueva orden de compra y devuelve el resumen.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito comprado correctamente"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "El carrito no puede ser comprado (vacío o inactivo)")
    })
    @PostMapping("/comprar-carrito/{idCarrito}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<OrdenResumenDTO>> comprarCarrito(
            @Parameter(description = "ID del carrito a comprar", required = true)
            @PathVariable Integer idCarrito) {

        log.info("POST /api/v1/ordenes/comprar-carrito/{}", idCarrito);

        OrdenCompra orden = ordenServicio.comprarDesdeCarrito(idCarrito);

        OrdenResumenDTO resumen = new OrdenResumenDTO(orden, tipoTicketRepositorio);

        return ResponseEntity.ok(StandardResponse.success(
                "Carrito comprado correctamente.",
                resumen
        ));
    }

    @Operation(
            summary = "Registrar asistentes para una orden pendiente",
            description = "Guarda los datos de los asistentes (nombre, DNI, etc.) en los tickets reservados de una orden.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistentes registrados correctamente"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o la orden no está pendiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/asistentes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> registrarAsistentes(
            @Parameter(description = "ID de la orden pendiente", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody RegistrarParticipantesDTO dto) {

        log.info("PUT /api/v1/ordenes/{}/asistentes", id);

        ordenServicio.registrarAsistentes(id, dto);
        return ResponseEntity.ok(StandardResponse.success("Asistentes registrados correctamente."));
    }
}