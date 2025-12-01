package pe.edu.pucp.fasticket.controllers.compra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.eventos.EventoResumenDTO;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.ItemCarrito;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.CarroComprasService;

import java.time.LocalDateTime;
import java.util.List;

@Tag(
    name = "Carrito de Compras",
    description = "API para gestión del carrito de compras. Requiere autenticación."
)
@RestController
@RequestMapping("/api/v1/carrito")
@RequiredArgsConstructor
@Slf4j
public class CarritoController {
    private final ClienteRepository clienteRepository;
    private final CarroComprasService carroComprasService;

    @Operation(
        summary = "Ver carrito del cliente",
        description = "Obtiene el carrito de compras actual del cliente",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Carrito obtenido",
            content = @Content(schema = @Schema(implementation = CarroComprasDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> verCarrito(
            @Parameter(description = "ID del cliente", required = true, example = "1")
            @PathVariable Integer idCliente) {
        
        log.info("GET /api/v1/carrito/cliente/{}", idCliente);
        CarroComprasDTO carrito = carroComprasService.verCarrito(idCliente);
        StandardResponse<CarroComprasDTO> response = StandardResponse.success("Carrito obtenido exitosamente", carrito);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Ver carrito del usuario autenticado",
        description = "Obtiene el carrito de compras del usuario autenticado",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Carrito obtenido",
            content = @Content(schema = @Schema(implementation = CarroComprasDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    @GetMapping("/items")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> verCarritoItems(
            @Parameter(description = "ID del cliente", required = true)
            @RequestParam Integer idCliente) {
        
        log.info("GET /api/v1/carrito/items - Cliente: {}", idCliente);
        CarroComprasDTO carrito = carroComprasService.verCarrito(idCliente);
        StandardResponse<CarroComprasDTO> response = StandardResponse.success("Items del carrito obtenidos exitosamente", carrito);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Agregar item(s) al carrito",
            description = """
        Agrega uno o más tipos de tickets al carrito del cliente.
        
        Soporta dos formatos:
        
        **Formato Simple** (un solo tipo de ticket):
```json
        {
          "idCliente": 6,
          "idTipoTicket": 12,
          "cantidad": 1
        }
```
        
        **Formato Múltiple** (varios tipos de tickets):
```json
        {
          "idCliente": 6,
          "items": [
            { "idTipoTicket": 12, "cantidad": 1 },
            { "idTipoTicket": 13, "cantidad": 2 }
          ]
        }
```
        """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item(s) agregado(s) exitosamente",
                    content = @Content(schema = @Schema(implementation = CarroComprasDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Cliente o tipo de ticket no encontrado")
    })
    @PostMapping("/items")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> agregarItem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del/los item(s) a agregar",
                    required = true,
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "Formato Simple",
                                            value = """
                            {
                              "idCliente": 6,
                              "idTipoTicket": 12,
                              "cantidad": 1
                            }
                            """
                                    ),
                                    @ExampleObject(
                                            name = "Formato Múltiple",
                                            value = """
                            {
                              "idCliente": 6,
                              "items": [
                                { "idTipoTicket": 12, "cantidad": 1 },
                                { "idTipoTicket": 13, "cantidad": 2 }
                              ]
                            }
                            """
                                    )
                            }
                    )
            )
            @RequestBody @Valid AddItemRequestDTO request) {

        String formato = request.esFormatoMultiple() ? "múltiple" : "simple";
        log.info("POST /api/v1/carrito/items [formato: {}] - Cliente: {}", formato, request.getIdCliente());

        CarroComprasDTO carritoActualizado = carroComprasService.agregarItemAlCarrito(request);
        StandardResponse<CarroComprasDTO> response = StandardResponse.success(
                "Item(s) agregado(s) al carrito exitosamente",
                carritoActualizado
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Eliminar UN TICKET individual del carrito",
            description = "Libera un ticket RESERVADO, lo devuelve a DISPONIBLE, y recalcula el total del carrito.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Ticket eliminado del carrito")
    @DeleteMapping("/tickets/{idTicket}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> eliminarTicketDelCarrito(
            @Parameter(description = "ID del Ticket individual a eliminar")
            @PathVariable Integer idTicket,
            @Parameter(description = "ID del cliente (para validación)")
            @RequestParam Integer idCliente) {

        log.info("DELETE /api/v1/carrito/tickets/{} - Cliente: {}", idTicket, idCliente);
        CarroComprasDTO carritoActualizado = carroComprasService.eliminarTicketIndividualDelCarrito(idTicket, idCliente);
        StandardResponse<CarroComprasDTO> response = StandardResponse.success("Ticket eliminado del carrito exitosamente", carritoActualizado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Aplicar un código promocional al carrito",
            description = "RF-027: Valida y aplica un cupón de descuento al total del carrito.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código aplicado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Código expirado, sin stock o inválido"),
            @ApiResponse(responseCode = "404", description = "Carrito o código no encontrado")
    })
    @PutMapping("/{idCarrito}/aplicar-cupon")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> aplicarCupon(
            @PathVariable Integer idCarrito,
            @RequestParam String codigo) {

        log.info("PUT /api/v1/carrito/{}/aplicar-cupon?codigo={}", idCarrito, codigo);
        CarroComprasDTO carritoActualizado = carroComprasService.aplicarCodigoPromocional(idCarrito, codigo);
        return ResponseEntity.ok(StandardResponse.success("Cupón aplicado.", carritoActualizado));
    }

    @Operation(
            summary = "Eliminar item del carrito",
            description = "Remueve un item del carrito de compras",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Item eliminado")
    @DeleteMapping("/items/{idItemCarrito}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CarroComprasDTO> eliminarItem(
            @Parameter(description = "ID del item a eliminar")
            @PathVariable Integer idItemCarrito,
            @Parameter(description = "ID del cliente")
            @RequestParam Integer idCliente) {

        log.info("DELETE /api/v1/carrito/items/{} - Cliente: {}", idItemCarrito, idCliente);
        CarroComprasDTO carritoActualizado = carroComprasService.eliminarItemDelCarrito(idItemCarrito, idCliente);
        return ResponseEntity.ok(carritoActualizado);
    }

    @Operation(
            summary = "Obtener información del evento del carrito",
            description = "Devuelve los detalles del evento (nombre, fecha, lugar) asociado a los tickets guardados en el carrito.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/{idCarrito}/evento-info")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<EventoResumenDTO>>> obtenerInfoEventoCarrito(
            @PathVariable Integer idCarrito) {

        List<EventoResumenDTO> eventos = carroComprasService.obtenerEventosDelCarrito(idCarrito);

        return ResponseEntity.ok(StandardResponse.success(
                "Información del evento recuperada",
                eventos
        ));
    }

    @Operation(
            summary = "Incrementar cantidad de un item",
            description = "Incrementa en 1 la cantidad de un item específico en el carrito usando su ID de item",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cantidad incrementada exitosamente",
                    content = @Content(schema = @Schema(implementation = CarroComprasDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Stock insuficiente, límite excedido o precio inconsistente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Item de carrito no encontrado"
            )
    })
    @PostMapping("/incrementar-item/{idItemCarrito}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> incrementarCantidadItem(
            @Parameter(description = "ID del item del carrito a incrementar", required = true)
            @PathVariable Integer idItemCarrito,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer idCliente = obtenerIdClienteDesdeAuth(userDetails);
        log.info("POST /api/v1/carrito/incrementar-item/{} - Cliente: {}", idItemCarrito, idCliente);

        // Nota: El servicio ahora recibe idItemCarrito
        CarroComprasDTO carritoActualizado = carroComprasService.incrementarCantidadItem(idCliente, idItemCarrito);

        StandardResponse<CarroComprasDTO> response = StandardResponse.success(
                "Ticket agregado exitosamente",
                carritoActualizado
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Decrementar cantidad de un item",
            description = "Decrementa en 1 la cantidad de un item. Si llega a 0, elimina el item del carrito.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cantidad decrementada o item eliminado exitosamente",
                    content = @Content(schema = @Schema(implementation = CarroComprasDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "No es posible decrementar (ej. cantidad ya es 0)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Item de carrito no encontrado"
            )
    })
    @PostMapping("/decrementar-item/{idItemCarrito}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> decrementarCantidadItem(
            @Parameter(description = "ID del item del carrito a decrementar", required = true)
            @PathVariable Integer idItemCarrito,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer idCliente = obtenerIdClienteDesdeAuth(userDetails);
        log.info("POST /api/v1/carrito/decrementar-item/{} - Cliente: {}", idItemCarrito, idCliente);

        // Nota: El servicio ahora recibe idItemCarrito
        CarroComprasDTO carritoActualizado = carroComprasService.decrementarCantidadItem(idCliente, idItemCarrito);

        StandardResponse<CarroComprasDTO> response = StandardResponse.success(
                "Ticket eliminado exitosamente",
                carritoActualizado
        );
        return ResponseEntity.ok(response);
    }
}

