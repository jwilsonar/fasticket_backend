package pe.edu.pucp.fasticket.controllers.eventos;

import java.util.List;
import java.util.stream.Collectors;

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
import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO;
import pe.edu.pucp.fasticket.dto.zonas.ZonaDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.mapper.ZonaMapper;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.services.S3Service;
import pe.edu.pucp.fasticket.services.eventos.ZonaServicio;

@Tag(
    name = "Zonas",
    description = "API para gestión de zonas dentro de los locales"
)
@RestController
@RequestMapping("/api/v1/zonas")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class ZonaController {

    private final ZonaServicio zonaServicio;
    private final ZonaMapper zonaMapper;
    private final S3Service s3Service;

    @Operation(
        summary = "Listar zonas",
        description = "Obtiene lista de zonas. Puede filtrar por local específico usando el parámetro 'local'."
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public ResponseEntity<StandardResponse<List<ZonaDTO>>> listar(
            @Parameter(description = "ID del local para filtrar zonas (opcional)")
            @RequestParam(required = false) Integer local) {
        
        log.info("GET /api/v1/zonas?local={}", local);
        
        List<Zona> zonas;
        if (local != null) {
            zonas = zonaServicio.buscarPorLocal(local);
        } else {
            zonas = zonaServicio.listarTodas();
        }
        
        List<ZonaDTO> zonasDTO = zonas.stream()
                .map(zonaMapper::toDTO)
                .collect(Collectors.toList());
        
        String mensaje = local != null 
            ? String.format("Zonas del local %d obtenidas exitosamente", local)
            : "Zonas obtenidas exitosamente";
            
        StandardResponse<List<ZonaDTO>> response = StandardResponse.success(mensaje, zonasDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Obtener zona por ID",
        description = "Obtiene información detallada de una zona específica"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Zona encontrada",
            content = @Content(schema = @Schema(implementation = ZonaDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Zona no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<ZonaDTO>> obtenerPorId(
            @Parameter(description = "ID de la zona")
            @PathVariable Integer id) {
        
        log.info("GET /api/v1/zonas/{}", id);
        return zonaServicio.buscarPorId(id)
                .map(zona -> {
                    ZonaDTO zonaDTO = zonaMapper.toDTO(zona);
                    StandardResponse<ZonaDTO> response = StandardResponse.success("Zona obtenida exitosamente", zonaDTO);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Crear zona",
        description = "Crea una nueva zona dentro de un local. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "Zona creada exitosamente",
            content = @Content(schema = @Schema(implementation = ZonaDTO.class))
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
            responseCode = "404", 
            description = "Local no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping(consumes = "application/json")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ZonaDTO>> crear(
            @Valid @RequestBody ZonaCreateDTO dto) {
        
        log.info("POST /api/v1/zonas - Nombre: {}, idLocal: {}", 
                dto.getNombre(), dto.getIdLocal());
        
        try {
            Zona zona = zonaMapper.toEntity(dto);
            log.info("Zona mapeada - Local: {}", zona.getLocal());
            Zona nuevaZona = zonaServicio.crear(zona, dto.getIdLocal());
            ZonaDTO zonaDTO = zonaMapper.toDTO(nuevaZona);
            
            log.info("ZonaDTO creado - idLocal: {}", zonaDTO.getIdLocal());
            StandardResponse<ZonaDTO> response = StandardResponse.success("Zona creada exitosamente", zonaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear zona: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al crear zona: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ZonaDTO>> crearConImagen(
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "aforoMax", required = false) Integer aforoMax,
            @RequestParam(value = "idLocal", required = false) Integer idLocal) {
        
        log.info("POST /api/v1/zonas/con-imagen - Nombre: {}", nombre != null ? nombre : "con imagen");
        
        try {
            if (nombre == null) {
                return ResponseEntity.badRequest()
                    .body(StandardResponse.error("Se requiere información de la zona"));
            }
            
            ZonaCreateDTO dto = new ZonaCreateDTO();
            dto.setNombre(nombre);
            dto.setAforoMax(aforoMax);
            dto.setIdLocal(idLocal);
            
            Zona zona = zonaMapper.toEntity(dto);
            log.info("Zona mapeada - Local: {}", zona.getLocal());
            Zona nuevaZona = zonaServicio.crear(zona, dto.getIdLocal());
            ZonaDTO zonaDTO = zonaMapper.toDTO(nuevaZona);
            
            // Subir imagen si se proporcionó
            if (imagen != null && !imagen.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imagen, "zonas", zonaDTO.getIdZona());
                zonaDTO.setImagenUrl(imageUrl);
            }
            
            log.info("ZonaDTO creado - idLocal: {}", zonaDTO.getIdLocal());
            StandardResponse<ZonaDTO> response = StandardResponse.success("Zona creada exitosamente", zonaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error al crear zona: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al crear zona: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Actualizar zona",
        description = "Actualiza información de una zona existente. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Zona actualizada exitosamente",
            content = @Content(schema = @Schema(implementation = ZonaDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos inválidos"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Zona no encontrada"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Sin permisos"
        )
    })
    @PutMapping(value = "/{id}", consumes = "application/json")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ZonaDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ZonaCreateDTO dto) {
        
        log.info("PUT /api/v1/zonas/{}", id);
        
        try {
            Zona zona = zonaMapper.toEntity(dto);
            zona.setIdZona(id);
            Zona actualizada = zonaServicio.actualizar(zona, dto.getIdLocal());
            ZonaDTO zonaDTO = zonaMapper.toDTO(actualizada);
            
            StandardResponse<ZonaDTO> response = StandardResponse.success("Zona actualizada exitosamente", zonaDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al actualizar zona: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al actualizar zona: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ZonaDTO>> actualizarConImagen(
            @PathVariable Integer id,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "aforoMax", required = false) Integer aforoMax,
            @RequestParam(value = "idLocal", required = false) Integer idLocal) {
        
        log.info("PUT /api/v1/zonas/{}/con-imagen", id);
        
        try {
            if (nombre == null) {
                return ResponseEntity.badRequest()
                    .body(StandardResponse.error("Se requiere información de la zona"));
            }
            
            ZonaCreateDTO dto = new ZonaCreateDTO();
            dto.setNombre(nombre);
            dto.setAforoMax(aforoMax);
            dto.setIdLocal(idLocal);
            
            Zona zona = zonaMapper.toEntity(dto);
            zona.setIdZona(id);
            Zona actualizada = zonaServicio.actualizar(zona, dto.getIdLocal());
            ZonaDTO zonaDTO = zonaMapper.toDTO(actualizada);
            
            // Subir imagen si se proporcionó
            if (imagen != null && !imagen.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imagen, "zonas", zonaDTO.getIdZona());
                zonaDTO.setImagenUrl(imageUrl);
            }
            
            StandardResponse<ZonaDTO> response = StandardResponse.success("Zona actualizada exitosamente", zonaDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al actualizar zona: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardResponse.error("Error al actualizar zona: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Eliminar zona",
        description = "Elimina una zona del sistema. Solo administradores.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Zona eliminada exitosamente"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Zona no encontrada"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Sin permisos"
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/zonas/{}", id);
        zonaServicio.eliminar(id);
        StandardResponse<String> response = StandardResponse.success("Zona eliminada exitosamente");
        return ResponseEntity.ok(response);
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
    @PostMapping("/{id}/imagen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirImagenZona(
            @Parameter(description = "ID de la zona", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Archivo de imagen", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("POST /api/v1/zonas/{}/imagen", id);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(StandardResponse.error("El archivo no puede estar vacío"));
        }
        
        try {
            String imageUrl = s3Service.uploadFile(file, "zonas", id);
            StandardResponse<String> response = StandardResponse.success("Imagen subida exitosamente", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir imagen de la zona {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
    }
}

