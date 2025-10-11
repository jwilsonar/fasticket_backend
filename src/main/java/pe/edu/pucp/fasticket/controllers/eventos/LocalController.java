package pe.edu.pucp.fasticket.controllers.eventos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import pe.edu.pucp.fasticket.dto.eventos.LocalCreateDTO;
import pe.edu.pucp.fasticket.dto.eventos.LocalResponseDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.services.eventos.LocalService;

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
public class LocalController {

    private final LocalService localService;

    @Operation(
        summary = "Listar todos los locales",
        description = "Obtiene lista de locales activos o todos según el parámetro. Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista obtenida exitosamente"
    )
    @GetMapping
    public ResponseEntity<StandardResponse<List<LocalResponseDTO>>> listar(
            @Parameter(description = "Mostrar solo locales activos", example = "true")
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        
        log.info("GET /api/v1/locales?soloActivos={}", soloActivos);
        List<LocalResponseDTO> locales = soloActivos 
            ? localService.listarActivos() 
            : localService.listarTodos();
        return ResponseEntity.ok(StandardResponse.success("Lista de locales obtenida exitosamente", locales));
    }

    @Operation(
        summary = "Obtener local por ID",
        description = "Obtiene información detallada de un local específico incluyendo dirección y capacidad. Endpoint público."
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
    public ResponseEntity<StandardResponse<LocalResponseDTO>> obtenerPorId(
            @Parameter(description = "ID del local", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/locales/{}", id);
        LocalResponseDTO local = localService.obtenerPorId(id);
        return ResponseEntity.ok(StandardResponse.success("Local obtenido exitosamente", local));
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
    public ResponseEntity<StandardResponse<LocalResponseDTO>> crear(@Valid @RequestBody LocalCreateDTO dto) {
        log.info("POST /api/v1/locales - Crear: {}", dto.getNombre());
        LocalResponseDTO response = localService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success("Local creado exitosamente", response));
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
    public ResponseEntity<StandardResponse<LocalResponseDTO>> actualizar(
            @Parameter(description = "ID del local a actualizar")
            @PathVariable Integer id,
            @Valid @RequestBody LocalCreateDTO dto) {
        
        log.info("PUT /api/v1/locales/{}", id);
        LocalResponseDTO response = localService.actualizar(id, dto);
        return ResponseEntity.ok(StandardResponse.success("Local actualizado exitosamente", response));
    }

    @Operation(
        summary = "Desactivar local",
        description = "Eliminación lógica del local (no se elimina físicamente). Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Local desactivado exitosamente"
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
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> eliminar(
            @Parameter(description = "ID del local a desactivar", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("DELETE /api/v1/locales/{}", id);
        localService.eliminarLogico(id);
        return ResponseEntity.ok(StandardResponse.success("Local desactivado exitosamente"));
    }

    @Operation(
        summary = "Buscar locales por nombre",
        description = "RF-005: Busca locales por nombre (búsqueda parcial). Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Locales encontrados"
    )
    @GetMapping("/buscar")
    public ResponseEntity<StandardResponse<List<LocalResponseDTO>>> buscarPorNombre(
            @Parameter(description = "Nombre del local a buscar", example = "Estadio")
            @RequestParam String nombre) {
        
        log.info("GET /api/v1/locales/buscar?nombre={}", nombre);
        List<LocalResponseDTO> locales = localService.buscarPorNombre(nombre);
        return ResponseEntity.ok(StandardResponse.success("Búsqueda completada exitosamente", locales));
    }

    @Operation(
        summary = "Buscar locales por distrito",
        description = "RF-005: Filtra locales por distrito. Endpoint público."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Locales encontrados en el distrito"
    )
    @GetMapping("/distrito/{idDistrito}")
    public ResponseEntity<StandardResponse<List<LocalResponseDTO>>> buscarPorDistrito(
            @Parameter(description = "ID del distrito", required = true, example = "1")
            @PathVariable Integer idDistrito) {
        
        log.info("GET /api/v1/locales/distrito/{}", idDistrito);
        List<LocalResponseDTO> locales = localService.buscarPorDistrito(idDistrito);
        return ResponseEntity.ok(StandardResponse.success("Locales obtenidos exitosamente", locales));
    }
}

