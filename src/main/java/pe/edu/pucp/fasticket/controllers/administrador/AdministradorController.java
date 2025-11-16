package pe.edu.pucp.fasticket.controllers.administrador;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import pe.edu.pucp.fasticket.dto.usuario.AdministradorPerfilResponseDTO;
import pe.edu.pucp.fasticket.dto.usuario.AdministradorPerfilUpdateDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.services.usuario.AdministradorService;

/**
 * Controlador para gestión de administradores.
 * Maneja perfiles y operaciones de administradores del sistema.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Tag(
    name = "Administrador",
    description = "API para gestión de perfiles de administrador. Requiere autenticación de administrador."
)
@RestController
@RequestMapping("/api/v1/administrador")
@RequiredArgsConstructor
@Slf4j
public class AdministradorController {

    private final AdministradorService administradorService;

    @Operation(
        summary = "Obtener perfil del administrador",
        description = "Obtiene información del perfil del administrador autenticado",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Perfil obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = AdministradorPerfilResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Administrador no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/perfil")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<AdministradorPerfilResponseDTO>> obtenerPerfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("GET /api/v1/administrador/perfil - Usuario: {}", userDetails.getUsername());
        AdministradorPerfilResponseDTO perfil = administradorService.obtenerPerfilPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(StandardResponse.success("Perfil obtenido exitosamente", perfil));
    }


    @Operation(
        summary = "Actualizar perfil del administrador",
        description = "Permite al administrador actualizar sus datos personales (nombres, apellidos, teléfono, dirección, cargo)",
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
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<AdministradorPerfilResponseDTO>> actualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AdministradorPerfilUpdateDTO dto) {
        
        log.info("PUT /api/v1/administrador/perfil - Usuario: {}", userDetails.getUsername());
        AdministradorPerfilResponseDTO perfilActualizado = administradorService.actualizarPerfil(userDetails.getUsername(), dto);
        return ResponseEntity.ok(StandardResponse.success("Perfil actualizado exitosamente", perfilActualizado));
    }

    @Operation(
        summary = "Desactivar administrador",
        description = "Realiza un borrado lógico de la cuenta de un administrador indicado por su ID",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Administrador desactivado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Administrador no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/desactivar/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> desactivarAdministrador(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Integer idAdmin) {

        log.warn("PUT /api/v1/administrador/desactivar/{} - Admin: {}", idAdmin, userDetails.getUsername());
        administradorService.desactivarAdmin(idAdmin);
        return ResponseEntity.ok(StandardResponse.success("Administrador desactivado exitosamente", null));
    }

    @Operation(
        summary = "Listar todos los administradores",
        description = "Devuelve la lista de todos los administradores del sistema (activos e inactivos)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = AdministradorPerfilResponseDTO.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado"
        )
    })
    @GetMapping("/listar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<AdministradorPerfilResponseDTO>>> listarAdministradores(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/administrador/listar - Usuario: {}", userDetails.getUsername());
        List<AdministradorPerfilResponseDTO> admins = administradorService.obtenerTodosLosAdmins();
        return ResponseEntity.ok(StandardResponse.success("Lista de administradores obtenida exitosamente", admins));
    }

}
