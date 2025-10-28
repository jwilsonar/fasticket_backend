package pe.edu.pucp.fasticket.controllers.administrador;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
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
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
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

}
