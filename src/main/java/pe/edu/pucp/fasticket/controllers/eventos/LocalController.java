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
import org.springframework.web.multipart.MultipartFile;

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
import pe.edu.pucp.fasticket.services.S3Service;
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
    private final S3Service s3Service;

    @Operation(
        summary = "Listar todos los locales",
        description = "Obtiene lista de locales activos o todos según el parámetro"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista obtenida exitosamente"
    )
    @GetMapping
    public ResponseEntity<StandardResponse<List<LocalResponseDTO>>> listar(
            @Parameter(description = "Mostrar solo activos")
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        
        log.info("GET /api/v1/locales?soloActivos={}", soloActivos);
        List<LocalResponseDTO> locales = soloActivos 
            ? localService.listarActivos() 
            : localService.listarTodos();
        StandardResponse<List<LocalResponseDTO>> response = StandardResponse.success("Locales obtenidos exitosamente", locales);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<StandardResponse<LocalResponseDTO>> obtenerPorId(
            @Parameter(description = "ID del local", required = true, example = "1")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/locales/{}", id);
        LocalResponseDTO local = localService.obtenerPorId(id);
        StandardResponse<LocalResponseDTO> response = StandardResponse.success("Local obtenido exitosamente", local);
        return ResponseEntity.ok(response);
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
    @PostMapping(consumes = "application/json")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<LocalResponseDTO>> crear(
            @Valid @RequestBody LocalCreateDTO dto) {
        
        log.info("POST /api/v1/locales - Crear: {}", dto.getNombre());
        
        try {
            LocalResponseDTO local = localService.crear(dto);
            StandardResponse<LocalResponseDTO> response = StandardResponse.success("Local creado exitosamente", local);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear local: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al crear local: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<LocalResponseDTO>> crearConImagen(
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "aforoTotal", required = false) Integer aforoTotal,
            @RequestParam(value = "idDistrito", required = false) Integer idDistrito) {
        
        log.info("POST /api/v1/locales/con-imagen - Crear: {}", nombre != null ? nombre : "con imagen");
        
        try {
            if (nombre == null) {
                return ResponseEntity.badRequest()
                    .body(StandardResponse.error("Se requiere información del local"));
            }
            
            LocalCreateDTO dto = new LocalCreateDTO();
            dto.setNombre(nombre);
            dto.setAforoTotal(aforoTotal);
            dto.setIdDistrito(idDistrito);
            
            LocalResponseDTO local = localService.crear(dto);
            
            // Subir imagen si se proporcionó
            if (imagen != null && !imagen.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imagen, "locales", local.getIdLocal());
                // Guardar la URL de la imagen en la base de datos
                local = localService.actualizarImagenUrl(local.getIdLocal(), imageUrl);
            }
            
            StandardResponse<LocalResponseDTO> response = StandardResponse.success("Local creado exitosamente", local);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear local: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al crear local: " + e.getMessage()));
        }
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
    @PutMapping(value = "/{id}", consumes = "application/json")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<LocalResponseDTO>> actualizar(
            @Parameter(description = "ID del local a actualizar")
            @PathVariable Integer id,
            @Valid @RequestBody LocalCreateDTO dto) {
        
        log.info("PUT /api/v1/locales/{}", id);
        
        try {
            LocalResponseDTO local = localService.actualizar(id, dto);
            StandardResponse<LocalResponseDTO> response = StandardResponse.success("Local actualizado exitosamente", local);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al actualizar local: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al actualizar local: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<LocalResponseDTO>> actualizarConImagen(
            @Parameter(description = "ID del local a actualizar")
            @PathVariable Integer id,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "aforoTotal", required = false) Integer aforoTotal,
            @RequestParam(value = "idDistrito", required = false) Integer idDistrito) {
        
        log.info("PUT /api/v1/locales/{}/con-imagen", id);
        
        try {
            if (nombre == null) {
                return ResponseEntity.badRequest()
                    .body(StandardResponse.error("Se requiere información del local"));
            }
            
            LocalCreateDTO dto = new LocalCreateDTO();
            dto.setNombre(nombre);
            dto.setAforoTotal(aforoTotal);
            dto.setIdDistrito(idDistrito);
            
            LocalResponseDTO local = localService.actualizar(id, dto);
            
            // Subir imagen si se proporcionó
            if (imagen != null && !imagen.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imagen, "locales", local.getIdLocal());
                // Guardar la URL de la imagen en la base de datos
                local = localService.actualizarImagenUrl(local.getIdLocal(), imageUrl);
            }
            
            StandardResponse<LocalResponseDTO> response = StandardResponse.success("Local actualizado exitosamente", local);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al actualizar local: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al actualizar local: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Desactivar local",
        description = "Eliminación lógica del local. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "204", description = "Local desactivado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/locales/{}", id);
        localService.eliminarLogico(id);
        StandardResponse<String> response = StandardResponse.success("Local eliminado exitosamente");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Subir imagen de local",
        description = "Sube una imagen para un local específico. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Imagen subida exitosamente",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Archivo inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "No autenticado"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Sin permisos (requiere rol ADMINISTRADOR)"
        )
    })
    @PostMapping("/{id}/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirImagenLocal(
            @Parameter(description = "ID del local", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Archivo de imagen", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("POST /api/v1/locales/{}/imagen", id);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(StandardResponse.error("El archivo no puede estar vacío"));
        }
        
        try {
            String imageUrl = s3Service.uploadFile(file, "locales", id);
            // Guardar la URL de la imagen en la base de datos
            localService.actualizarImagenUrl(id, imageUrl);
            StandardResponse<String> response = StandardResponse.success("Imagen subida exitosamente", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imagen del local {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
    }
}



