package pe.edu.pucp.fasticket.controllers.fidelizacion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import pe.edu.pucp.fasticket.dto.fidelizacion.*;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.exception.ResourceNotFoundException;
import pe.edu.pucp.fasticket.repository.usuario.ClienteRepository;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

@Tag(
    name = "Fidelización - Cliente",
    description = "API para gestión de fidelización para clientes (consultar puntos, realizar canjes)."
)
@RestController
@RequestMapping("/api/v1/cliente/fidelizacion")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class FidelizacionClienteController {

    private final FidelizacionService fidelizacionService;
    private final ClienteRepository clienteRepository;

    @Operation(
        summary = "Obtener puntos acumulados del cliente autenticado",
        description = """
            Calcula y retorna los puntos acumulados totales del cliente autenticado.
            
            Este endpoint calcula automáticamente los puntos del cliente basándose en:
            - Puntos ganados por compras realizadas
            - Puntos perdidos por canjes realizados
            
            Los puntos ganados se suman y los perdidos se restan para obtener el total acumulado.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Puntos obtenidos exitosamente. Retorna el ID del cliente, puntos acumulados y mensaje de confirmación.",
            content = @Content(schema = @Schema(implementation = PuntosAcumuladosResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Cliente no encontrado con el email del token JWT.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol CLIENTE.")
    })
    @GetMapping("/puntos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<PuntosAcumuladosResponseDTO>> obtenerPuntosAcumulados(
        @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/cliente/fidelizacion/puntos - Usuario: {}", userDetails.getUsername());
        
        var cliente = clienteRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + userDetails.getUsername()));
        
        Integer puntos = fidelizacionService.calcularPuntosAcumulados(cliente.getIdPersona());
        
        PuntosAcumuladosResponseDTO response = new PuntosAcumuladosResponseDTO();
        response.setIdCliente(cliente.getIdPersona());
        response.setPuntosAcumulados(puntos);
        response.setMensaje("Puntos obtenidos exitosamente");
        
        return ResponseEntity.ok(StandardResponse.success("Puntos obtenidos exitosamente.", response));
    }

    @Operation(
        summary = "Listar historial de puntos del cliente autenticado",
        description = """
            Retorna el historial completo de transacciones de puntos del cliente autenticado.
            
            Incluye todas las transacciones de puntos (ganados y perdidos) con información como:
            - Cantidad de puntos
            - Fecha de transacción
            - Tipo de transacción (GANADO o PERDIDO)
            - Fecha de vencimiento (para puntos por compra)
            - Estado (activo/inactivo)
            - Regla de puntos aplicada
            
            Las transacciones están ordenadas por fecha, siendo las más recientes primero.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Historial obtenido exitosamente. Lista puede estar vacía si el cliente no tiene transacciones.",
            content = @Content(schema = @Schema(implementation = PuntosDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol CLIENTE."),
        @ApiResponse(
            responseCode = "404", 
            description = "Cliente no encontrado con el email del token JWT.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/historial-puntos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<PuntosDTO>>> obtenerHistorialPuntos(
        @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/cliente/fidelizacion/historial-puntos - Usuario: {}", userDetails.getUsername());
        
        var cliente = clienteRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + userDetails.getUsername()));

        List<PuntosDTO> historial = fidelizacionService.listarPuntosActivosPorCliente(cliente.getIdPersona());
        
        return ResponseEntity.ok(StandardResponse.success("Historial obtenido exitosamente.", historial));
    }

    @Operation(
        summary = "Listar canjes realizados por el cliente autenticado",
        description = """
            Retorna el historial de canjes realizados por el cliente autenticado.
            
            Un canje representa el uso de puntos acumulados para obtener un descuento en una compra.
            Cada canje incluye:
            - ID del canje
            - Fecha del canje
            - Puntos utilizados
            - Orden de compra asociada
            - Descuento aplicado
            
            Los canjes están ordenados por fecha, siendo los más recientes primero.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Canjes obtenidos exitosamente. Lista puede estar vacía si el cliente no tiene canjes.",
            content = @Content(schema = @Schema(implementation = CanjeDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol CLIENTE."),
        @ApiResponse(
            responseCode = "404", 
            description = "Cliente no encontrado con el email del token JWT.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/canjes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<CanjeDTO>>> obtenerCanjes(
        @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/cliente/fidelizacion/canjes - Usuario: {}", userDetails.getUsername());
        
        var cliente = clienteRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + userDetails.getUsername()));
        
        List<CanjeDTO> canjes = fidelizacionService.listarCanjesPorCliente(cliente.getIdPersona());
        
        return ResponseEntity.ok(StandardResponse.success("Canjes obtenidos exitosamente.", canjes));
    }

    @Operation(
        summary = "Realizar un canje de puntos",
        description = """
            Permite al cliente autenticado canjear sus puntos acumulados por un descuento total en su compra.
            
            REGLAS DE NEGOCIO:
            - Solo se puede canjear puntos en órdenes con estado PENDIENTE
            - Los puntos canjeados deben cubrir el TOTAL de la orden (después del descuento por membresía)
            - El descuento aplicado será exactamente el total de la orden, dejando el total en 0
            - Si se canjea puntos, NO se pueden aplicar códigos promocionales adicionales (mutuamente excluyentes)
            - El cliente debe tener suficientes puntos acumulados
            - Los puntos canjeados se marcan como PERDIDO en el historial
            
            PROCESO:
            1. El sistema calcula cuántos puntos se necesitan según la regla de canje activa
            2. Valida que el cliente tenga suficientes puntos
            3. Aplica el descuento completo a la orden
            4. Registra la transacción de puntos perdidos
            5. Crea el registro de canje asociado a la orden
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Canje realizado exitosamente. La orden queda con total 0.",
            content = @Content(schema = @Schema(implementation = CanjeDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = """
                Error en la validación:
                - Puntos insuficientes
                - La cantidad de puntos no coincide con lo necesario
                - El monto de descuento no coincide con el total de la orden
                - La orden no está en estado PENDIENTE
                - No hay reglas de canje activas
                """,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(
            responseCode = "403", 
            description = "Usuario no tiene rol CLIENTE o intenta canjear puntos de otro cliente."
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Cliente u orden de compra no encontrada.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/canje")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CanjeDTO>> realizarCanje(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody CanjeRequestDTO request) {

        log.info("POST /api/v1/cliente/fidelizacion/canje - Usuario: {}", userDetails.getUsername());
        
        var cliente = clienteRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + userDetails.getUsername()));
        
        // Asegurarse de que el ID del cliente en el request coincida con el del usuario autenticado
        if (!request.getIdCliente().equals(cliente.getIdPersona())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                StandardResponse.error("No tienes permisos para realizar canjes en nombre de otro cliente.", null)
            );
        }
        
        CanjeDTO canje = fidelizacionService.realizarCanje(request);
        
        return ResponseEntity.ok(StandardResponse.success("Canje realizado exitosamente.", canje));
    }

    @Operation(
        summary = "Verificar un código promocional",
        description = """
            Permite verificar si un código promocional es válido y obtener su información.
            
            Este endpoint devuelve los detalles del código promocional sin aplicarlo, incluyendo:
            - Descripción
            - Tipo de descuento (MONTO_FIJO o PORCENTAJE)
            - Valor del descuento
            - Fecha de expiración
            - Stock disponible
            - Cantidad por cliente permitida
            
            NOTA: Para aplicar el descuento, se debe usar el endpoint específico de compras.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Código verificado exitosamente. Retorna todos los detalles del código promocional.",
            content = @Content(schema = @Schema(implementation = CodigoPromocionalDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Código promocional no encontrado con el código proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol CLIENTE.")
    })
    @GetMapping("/codigo-promocional/{codigo}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<CodigoPromocionalDTO>> verificarCodigoPromocional(
        @Parameter(description = "Código promocional", required = true)
        @PathVariable String codigo) {

        log.info("GET /api/v1/cliente/fidelizacion/codigo-promocional/{}", codigo);
        
        try {
            CodigoPromocionalDTO codigoPromo = fidelizacionService.obtenerPorCodigo(codigo);
            return ResponseEntity.ok(StandardResponse.success("Código promocional verificado exitosamente.", codigoPromo));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                StandardResponse.error("Código promocional no encontrado.", null)
            );
        }
    }
}

