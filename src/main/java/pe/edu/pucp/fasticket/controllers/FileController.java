package pe.edu.pucp.fasticket.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.services.S3Service;

import java.util.List;

@Tag(
    name = "Archivos",
    description = "API para gestión de archivos e imágenes. Subida a S3."
)
@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final S3Service s3Service;

    @Operation(
        summary = "Subir imagen de evento",
        description = "Sube una imagen para un evento específico. Solo administradores.",
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
    @PostMapping("/eventos/{eventoId}/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirImagenEvento(
            @Parameter(description = "ID del evento", required = true)
            @PathVariable @NotNull Integer eventoId,
            @Parameter(description = "Archivo de imagen", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("POST /api/v1/files/eventos/{}/imagen", eventoId);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(StandardResponse.error("El archivo no puede estar vacío"));
        }
        
        try {
            String imageUrl = s3Service.uploadFile(file, "eventos", eventoId);
            StandardResponse<String> response = StandardResponse.success("Imagen subida exitosamente", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imagen del evento {}: {}", eventoId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
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
    @PostMapping("/locales/{localId}/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirImagenLocal(
            @Parameter(description = "ID del local", required = true)
            @PathVariable @NotNull Integer localId,
            @Parameter(description = "Archivo de imagen", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("POST /api/v1/files/locales/{}/imagen", localId);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(StandardResponse.error("El archivo no puede estar vacío"));
        }
        
        try {
            String imageUrl = s3Service.uploadFile(file, "locales", localId);
            StandardResponse<String> response = StandardResponse.success("Imagen subida exitosamente", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imagen del local {}: {}", localId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Subir imagen de zona",
        description = "Sube una imagen para una zona específica. Solo administradores.",
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
    @PostMapping("/zonas/{zonaId}/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirImagenZona(
            @Parameter(description = "ID de la zona", required = true)
            @PathVariable @NotNull Integer zonaId,
            @Parameter(description = "Archivo de imagen", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("POST /api/v1/files/zonas/{}/imagen", zonaId);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(StandardResponse.error("El archivo no puede estar vacío"));
        }
        
        try {
            String imageUrl = s3Service.uploadFile(file, "zonas", zonaId);
            StandardResponse<String> response = StandardResponse.success("Imagen subida exitosamente", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imagen de la zona {}: {}", zonaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Subir múltiples imágenes",
        description = "Sube múltiples imágenes para una entidad específica. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Imágenes subidas exitosamente",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Archivos inválidos",
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
    @PostMapping("/{tipo}/{entityId}/imagenes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<String>>> subirImagenes(
            @Parameter(description = "Tipo de entidad (eventos, locales, zonas)", required = true)
            @PathVariable String tipo,
            @Parameter(description = "ID de la entidad", required = true)
            @PathVariable @NotNull Integer entityId,
            @Parameter(description = "Archivos de imagen", required = true)
            @RequestParam("files") List<MultipartFile> files) {
        
        log.info("POST /api/v1/files/{}/{}/imagenes", tipo, entityId);
        
        if (files.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(StandardResponse.error("Debe proporcionar al menos un archivo"));
        }
        
        try {
            List<String> imageUrls = s3Service.uploadFiles(files, tipo, entityId);
            StandardResponse<List<String>> response = StandardResponse.success("Imágenes subidas exitosamente", imageUrls);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imágenes de {} {}: {}", tipo, entityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al subir las imágenes: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Eliminar imagen",
        description = "Elimina una imagen de S3. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Imagen eliminada exitosamente"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "URL inválida",
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
    @DeleteMapping("/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> eliminarImagen(
            @Parameter(description = "URL de la imagen a eliminar", required = true)
            @RequestParam("url") String imageUrl) {
        
        log.info("DELETE /api/v1/files/imagen?url={}", imageUrl);
        
        try {
            boolean deleted = s3Service.deleteFile(imageUrl);
            if (deleted) {
                StandardResponse<String> response = StandardResponse.success("Imagen eliminada exitosamente", imageUrl);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StandardResponse.error("No se pudo eliminar la imagen"));
            }
        } catch (Exception e) {
            log.error("Error al eliminar imagen {}: {}", imageUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al eliminar la imagen: " + e.getMessage()));
        }
    }
}
