package pe.edu.pucp.fasticket.controllers.compra;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.services.CarroComprasService;

@Tag(
    name = "Carrito de Compras",
    description = "API para gestión del carrito de compras. Requiere autenticación."
)
@RestController
@RequestMapping("/api/v1/carrito")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class CarritoController {

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
    public ResponseEntity<CarroComprasDTO> verCarrito(
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
        summary = "Agregar item al carrito",
        description = "Agrega un tipo de ticket al carrito del cliente",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Item agregado exitosamente",
            content = @Content(schema = @Schema(implementation = CarroComprasDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping("/items")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CarroComprasDTO> agregarItem(@RequestBody AddItemRequestDTO request) {
        log.info("POST /api/v1/carrito/items - Cliente: {}, Tipo Ticket: {}", 
                 request.getIdCliente(), request.getIdTipoTicket());
        CarroComprasDTO carritoActualizado = carroComprasService.agregarItemAlCarrito(request);
        StandardResponse<CarroComprasDTO> response = StandardResponse.success("Item agregado al carrito exitosamente", carritoActualizado);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Eliminar item del carrito",
        description = "Remueve un item del carrito de compras",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Item eliminado")
    @DeleteMapping("/items/{idItemCarrito}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CarroComprasDTO>> eliminarItem(
            @Parameter(description = "ID del item a eliminar")
            @PathVariable Integer idItemCarrito,
            @Parameter(description = "ID del cliente")
            @RequestParam Integer idCliente) {
        
        log.info("DELETE /api/v1/carrito/items/{} - Cliente: {}", idItemCarrito, idCliente);
        CarroComprasDTO carritoActualizado = carroComprasService.eliminarItemDelCarrito(idItemCarrito, idCliente);
        StandardResponse<CarroComprasDTO> response = StandardResponse.success("Item eliminado del carrito exitosamente", carritoActualizado);
        return ResponseEntity.ok(response);
    }
}

