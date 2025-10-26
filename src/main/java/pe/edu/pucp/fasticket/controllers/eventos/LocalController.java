package pe.edu.pucp.fasticket.controllers.eventos;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.services.eventos.LocalService;

import java.util.List;

@Tag(
    name = "Locales",
    description = "API para gestión de locales donde se realizan eventos. " +
                  "Requiere autenticación de administrador para crear/modificar."
)
@RestController
@RequestMapping("/api/v1/locales")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/locales")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class LocalController {

    private final LocalService localService;

    @Operation(
        summary = "Listar todos los locales",
        description = "Obtiene lista de locales activos o todos según el parámetro"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista obtenida exitosamente"
    )
    @GetMapping
    public ResponseEntity<List<LocalResponseDTO>> listar(
            @Parameter(description = "Mostrar solo activos")
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        
        log.info("GET /api/v1/locales?soloActivos={}", soloActivos);
        List<LocalResponseDTO> locales = soloActivos 
            ? localService.listarActivos() 
            : localService.listarTodos();
        return ResponseEntity.ok(locales);
    }

    @Operation(
        summary = "Obtener local por ID",
        description = "Obtiene información detallada de un local específico"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Local encontrado",
            content = @Content(schema = @Schema(implementation = LocalResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Local no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<LocalResponseDTO> obtenerPorId(
            @Parameter(description = "ID del local", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/locales/{}", id);
        LocalResponseDTO local = localService.obtenerPorId(id);
        return ResponseEntity.ok(local);
    }

    @Operation(
        summary = "Crear nuevo local",
        description = "Crea un local para realizar eventos. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Local creado",
            content = @Content(schema = @Schema(implementation = LocalResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos (requiere rol ADMINISTRADOR)"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Ya existe un local con ese nombre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LocalResponseDTO> crear(@Valid @RequestBody LocalCreateDTO dto) {
        log.info("POST /api/v1/locales - Crear: {}", dto.getNombre());
        LocalResponseDTO response = localService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Actualizar local",
        description = "Actualiza información de un local existente. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Local actualizado",
            content = @Content(schema = @Schema(implementation = LocalResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Local no encontrado"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permisos"
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LocalResponseDTO> actualizar(
            @Parameter(description = "ID del local a actualizar")
            @PathVariable Integer id,
            @Valid @RequestBody LocalCreateDTO dto) {
        
        log.info("PUT /api/v1/locales/{}", id);
        LocalResponseDTO response = localService.actualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Desactivar local",
        description = "Eliminación lógica del local. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "204", description = "Local desactivado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/locales/{}", id);
        localService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }
}



