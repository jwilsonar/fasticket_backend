package pe.edu.pucp.fasticket.controllers.eventos;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO;
import pe.edu.pucp.fasticket.dto.zonas.ZonaDTO;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.mapper.ZonaMapper;
import pe.edu.pucp.fasticket.model.eventos.Zona;
import pe.edu.pucp.fasticket.services.eventos.ZonaServicio;

@Tag(
    name = "Zonas",
    description = "API para gestión de zonas dentro de los locales"
)
@RestController
@RequestMapping("/api/v1/zonas")
@RequiredArgsConstructor
@Slf4j
public class ZonaController {

    private final ZonaServicio zonaServicio;
    private final ZonaMapper zonaMapper;

    @Operation(
        summary = "Listar zonas",
        description = "Obtiene lista de zonas. Puede filtrar por local específico usando el parámetro 'local'."
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public ResponseEntity<StandardResponse<List<ZonaDTO>>> listar(
            @Parameter(description = "ID del evento para filtrar zonas")
            @RequestParam(value = "evento", required = false) Integer idEvento) {

        List<Zona> zonas;
        String mensaje;

        if (idEvento != null) {
            // 2. Llama al servicio usando la variable 'idEvento'
            log.info("GET /api/v1/zonas?evento={}", idEvento);
            zonas = zonaServicio.buscarPorEvento(idEvento);
            mensaje = "Zonas del evento " + idEvento + " obtenidas exitosamente";

        } else {
            // 3. Si NO envían parámetro, lista todo
            log.info("GET /api/v1/zonas (todas)");
            zonas = zonaServicio.listarTodas();
            mensaje = "Zonas obtenidas exitosamente";
        }

        List<ZonaDTO> zonasDTO = zonas.stream()
                .map(zonaMapper::toDTO)
                .collect(Collectors.toList());
            
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

        log.info("POST /api/v1/zonas - Nombre: {}, idEvento: {}",
                dto.getNombre(), dto.getIdEvento());

        try {
            Zona zona = zonaMapper.toEntity(dto);
            Zona nuevaZona = zonaServicio.crear(zona, dto.getIdEvento());
            ZonaDTO zonaDTO = zonaMapper.toDTO(nuevaZona);
            log.info("ZonaDTO creado - idEvento: {}", zonaDTO.getIdEvento());
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
            Zona actualizada = zonaServicio.actualizar(zona, dto.getIdEvento());
            ZonaDTO zonaDTO = zonaMapper.toDTO(actualizada);
            
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
}