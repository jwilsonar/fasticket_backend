package pe.edu.pucp.fasticket.controllers.usuario;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilUpdateDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.services.usuario.ClienteService;

/**
 * Controlador para gestión de clientes.
 * Implementa RF-030, RF-032, RF-060, RF-091.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Tag(
    name = "Clientes",
    description = "API para gestión de perfiles de clientes e historial de compras. Requiere autenticación."
)
@RestController
@RequestMapping("/api/v1/clientes")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(
        summary = "Obtener perfil del cliente",
        description = "RF-030: Obtiene información del perfil del cliente autenticado",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Perfil obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = ClientePerfilResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<ClientePerfilResponseDTO>> obtenerPerfil( // por email
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("GET /api/v1/clientes/perfil - Usuario: {}", userDetails.getUsername());
        ClientePerfilResponseDTO perfil = clienteService.obtenerPerfilPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(StandardResponse.success("Perfil obtenido exitosamente", perfil));
    }

    @Operation(
        summary = "Obtener perfil por ID",
        description = "Obtiene información del perfil de un cliente específico. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Perfil obtenido exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente no encontrado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @GetMapping("/{id}/perfil")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ClientePerfilResponseDTO>> obtenerPerfilPorId(
            @Parameter(description = "ID del cliente", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/clientes/{}/perfil", id);
        ClientePerfilResponseDTO perfil = clienteService.obtenerPerfilPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Perfil obtenido exitosamente", perfil));
    }

    @Operation(
        summary = "Actualizar perfil del cliente",
        description = "RF-060: Permite al cliente actualizar sus datos personales (nombres, apellidos, teléfono, dirección)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Perfil actualizado exitosamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        )
    })
    @PutMapping("/perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<ClientePerfilResponseDTO>> actualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ClientePerfilUpdateDTO dto) {
        
        log.info("PUT /api/v1/clientes/perfil - Usuario: {}", userDetails.getUsername());
        ClientePerfilResponseDTO perfilActualizado = clienteService.actualizarPerfil(userDetails.getUsername(), dto);
        return ResponseEntity.ok(StandardResponse.success("Perfil actualizado exitosamente", perfilActualizado));
    }

    @Operation(
        summary = "Historial de compras del cliente",
        description = "RF-032, RF-091: Obtiene el historial completo de compras del cliente autenticado",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Historial obtenido exitosamente"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        )
    })
    @GetMapping("/historial-compras")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<OrdenCompra>>> obtenerHistorialCompras(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("GET /api/v1/clientes/historial-compras - Usuario: {}", userDetails.getUsername());
        List<OrdenCompra> historial = clienteService.obtenerHistorialCompras(userDetails.getUsername());
        return ResponseEntity.ok(StandardResponse.success("Historial de compras obtenido exitosamente", historial));
    }

    @Operation(
        summary = "Historial de compras por ID",
        description = "Obtiene el historial de compras de un cliente específico. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Historial obtenido exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente no encontrado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @GetMapping("/{id}/historial-compras")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<OrdenCompra>>> obtenerHistorialComprasPorId(
            @Parameter(description = "ID del cliente", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/clientes/{}/historial-compras", id);
        List<OrdenCompra> historial = clienteService.obtenerHistorialComprasPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Historial de compras obtenido exitosamente", historial));
    }

    @Operation(
        summary = "Obtener clientes por nivel",
        description = "Devuelve los perfiles de clientes filtrados por su nivel. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfiles obtenidos exitosamente",
                content = @Content(schema = @Schema(implementation = ClientePerfilResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "No se encontraron clientes para el nivel especificado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/nivel/{nivel}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<ClientePerfilResponseDTO>>> obtenerClientesPerfilPorNivel(
            @Parameter(description = "Nivel del cliente (ej: BRONCE, PLATA, ORO)", required = true, example = "BRONCE")
            @PathVariable TipoMembresia nivel) {

        log.info("GET /api/v1/clientes/nivel/{} - Obtener clientes por nivel", nivel);
        List<ClientePerfilResponseDTO> perfiles = clienteService.obtenerPerfilesPorNivel(nivel);
        return ResponseEntity.ok(StandardResponse.success("Perfiles obtenidos exitosamente", perfiles));
    }
}

