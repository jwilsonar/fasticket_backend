package pe.edu.pucp.fasticket.controllers.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
import pe.edu.pucp.fasticket.dto.eventos.EventoResponseDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilEditDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilUpdateDTO;
import pe.edu.pucp.fasticket.dto.usuario.ClientePerfilResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.services.usuario.ClienteService;
import pe.edu.pucp.fasticket.dto.compra.TransferenciaRequestDTO;
import pe.edu.pucp.fasticket.services.compra.TransferenciaService;

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
    private final TransferenciaService transferenciaService;

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

    /////////////////////////////////////
    //CAMBIOS YO QUE SON MUY CAMBIANTES//
    /////////////////////////////////////

    @Operation(
            summary = "Editar perfil del cliente",
            description = "RF-060: Permite al administrador editar datos de un cliente",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil editado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos"
            )
    })
    @PutMapping("/{id}/perfil")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ClientePerfilResponseDTO>> editarPerfil(
            @Parameter(description = "ID del cliente", required = true, example = "7")
            @PathVariable Integer id,
            @Valid @RequestBody ClientePerfilEditDTO dto) {

        log.info("PUT /api/v1/clientes/{}/perfil", id);
        ClientePerfilResponseDTO perfilActualizado = clienteService.editarPerfil(id,dto);
        return ResponseEntity.ok(StandardResponse.success("Perfil editado exitosamente", perfilActualizado));
    }

    @Operation(
            summary = "Lista de clientes",
            description = "Lista a todos los clientes. Solo administradores.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos"
            )
    })
    @GetMapping("/listar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<ClientePerfilResponseDTO>>> listarClientes() {

        log.info("GET /api/v1/clientes/listar");
        List<ClientePerfilResponseDTO> listaClientes = clienteService.listarTodos();
        return ResponseEntity.ok(StandardResponse.success("Lista de clientes obtenida exitosamente", listaClientes));
    }

    @Operation(
            summary = "Desactivar (borrado lógico) de un cliente",
            description = "Permite al administrador desactivar la cuenta de un cliente. El cliente no será borrado, solo marcado como inactivo.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente desactivado exitosamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El cliente ya estaba desactivado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos (requiere rol ADMIN)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente no encontrado"
            )
    })
    @DeleteMapping("/{idCliente}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> desactivarCliente(
            @Parameter(description = "ID del cliente a desactivar", required = true)
            @PathVariable Integer idCliente) {

        log.info("DELETE /api/v1/clientes/{}", idCliente);

        clienteService.desactivarCliente(idCliente);

        return ResponseEntity.ok(StandardResponse.success("Cliente desactivado exitosamente."));
    }

    @Operation(
            summary = "Marcar un cliente como verificado",
            description = "RF-031: Permite al administrador marcar la cuenta de un cliente como verificada (correo/teléfono).",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente marcado como verificado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El cliente ya estaba verificado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sin permisos (requiere rol ADMIN)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente no encontrado"
            )
    })
    @PutMapping("/{idCliente}/verificar") // <-- Endpoint Lógico
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ClientePerfilResponseDTO>> marcarClienteComoVerificado(
            @Parameter(description = "ID del cliente a verificar", required = true)
            @PathVariable Integer idCliente) {

        log.info("PUT /api/v1/clientes/{}/verificar", idCliente);

        ClientePerfilResponseDTO perfilActualizado = clienteService.marcarComoVerificado(idCliente);

        return ResponseEntity.ok(StandardResponse.success("Cliente marcado como verificado exitosamente.", perfilActualizado));
    }

    @Operation(
            summary = "Obtener lista de eventos favoritos del cliente",
            description = "RF-074: Obtiene la lista de eventos que el cliente autenticado ha marcado como favoritos.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/favoritos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<List<EventoResponseDTO>>> obtenerFavoritos(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/clientes/favoritos - Usuario: {}", userDetails.getUsername());
        List<EventoResponseDTO> favoritos = clienteService.listarFavoritos(userDetails.getUsername());
        return ResponseEntity.ok(StandardResponse.success("Favoritos obtenidos exitosamente", favoritos));
    }

    @Operation(
            summary = "Agregar un evento a favoritos",
            description = "RF-074: Marca un evento como favorito para el cliente autenticado.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/favoritos/{idEvento}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> agregarFavorito(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID del evento a marcar", required = true)
            @PathVariable Integer idEvento) {

        log.info("POST /api/v1/clientes/favoritos/{} - Usuario: {}", idEvento, userDetails.getUsername());
        clienteService.agregarFavorito(userDetails.getUsername(), idEvento);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success("Evento añadido a favoritos.", null));
    }

    @Operation(
            summary = "Quitar un evento de favoritos",
            description = "RF-074: Desmarca un evento como favorito para el cliente autenticado.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/favoritos/{idEvento}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> quitarFavorito(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID del evento a desmarcar", required = true)
            @PathVariable Integer idEvento) {

        log.info("DELETE /api/v1/clientes/favoritos/{} - Usuario: {}", idEvento, userDetails.getUsername());
        clienteService.quitarFavorito(userDetails.getUsername(), idEvento);
        return ResponseEntity.ok(StandardResponse.success("Evento quitado de favoritos.", null));
    }

    @Operation(
            summary = "Transferir una entrada (ticket) a otro cliente",
            description = "RF-092: Permite al cliente autenticado (emisor) transferir la propiedad de un ticket a otro cliente (receptor) usando su email.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferencia exitosa"),
            @ApiResponse(responseCode = "400", description = "Regla de negocio violada (ej. no transferible, mismo usuario)"),
            @ApiResponse(responseCode = "403", description = "No eres el dueño de este ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket o Cliente Receptor no encontrado")
    })
    @PostMapping("/tickets/{idTicket}/transferir")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StandardResponse<Void>> transferirTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID del ticket a transferir", required = true)
            @PathVariable Integer idTicket,
            @Valid @RequestBody TransferenciaRequestDTO request) {

        log.info("POST /api/v1/clientes/tickets/{}/transferir - Usuario: {}", idTicket, userDetails.getUsername());

        transferenciaService.transferirTicket(idTicket, userDetails.getUsername(), request);

        return ResponseEntity.ok(StandardResponse.success("Ticket transferido exitosamente.", null));
    }
}

