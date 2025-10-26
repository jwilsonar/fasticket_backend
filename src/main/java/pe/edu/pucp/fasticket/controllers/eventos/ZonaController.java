// RUTA: pe.edu.pucp.fasticket.controllers.eventos.ZonaController.java 
// (O mejor, muévelo a controllers.zonas si creas ese paquete)

package pe.edu.pucp.fasticket.controllers.eventos; // Cambia si mueves el archivo

import java.util.List;
import java.util.Optional; // Necesario para el Optional que devuelve el servicio

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Usar *

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
// --- CORRECCIÓN DE IMPORTS ---
import pe.edu.pucp.fasticket.dto.zonas.ZonaDTO; // Importar DTO
import pe.edu.pucp.fasticket.dto.zonas.ZonaCreateDTO; // Importar DTO
// --- FIN CORRECCIÓN ---
import pe.edu.pucp.fasticket.services.zonas.ZonaService; // Servicio correcto

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

    private final ZonaService zonaService; // Servicio correcto inyectado

    @Operation(summary = "Listar todas las zonas")
    @GetMapping
    public ResponseEntity<StandardResponse<List<ZonaDTO>>> listar() { // Devuelve DTO
        log.info("GET /api/v1/zonas");
        // --- CORRECCIÓN DE MÉTODO ---
        List<ZonaDTO> zonas = zonaService.ListarZonas(); // Llama al método correcto del Service
        // --- FIN CORRECCIÓN ---
        return ResponseEntity.ok(StandardResponse.success("Lista de zonas obtenida", zonas));
    }

    @Operation(summary = "Obtener zona por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zona encontrada", content = @Content(schema = @Schema(implementation = ZonaDTO.class))), // Devuelve DTO
            @ApiResponse(responseCode = "404", description = "Zona no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<ZonaDTO>> obtenerPorId(@PathVariable Integer id) {
        log.info("GET /api/v1/zonas/{}", id);
        // --- CORRECCIÓN DE MÉTODO ---
        ZonaDTO zona = zonaService.BuscarId(id); // Llama al método correcto (asumiendo que devuelve DTO y lanza 404 si no encuentra)
        // --- FIN CORRECCIÓN ---
        return ResponseEntity.ok(StandardResponse.success("Zona obtenida", zona));
        // El .orElse(notFound()) ya no es necesario si el service lanza excepción
    }

    // --- NO SE NECESITA UN POST DIRECTO PARA ZONA ---
    // La creación de Zona ahora se hace a través del EventoController 
    // (@PostMapping("/{idEvento}/zonas")) para asociarla a un evento.
    // Si necesitas un endpoint para crear zonas independientes (no ligadas a un evento), 
    // puedes mantener este, pero debe usar ZonaCreateDTO y llamar a un método diferente en ZonaService.
    /* @Operation(summary = "Crear zona independiente (USO LIMITADO)")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ZonaDTO>> crearIndependiente(@Valid @RequestBody ZonaCreateDTO dto) { // Usa CreateDTO
        log.info("POST /api/v1/zonas - Crear zona independiente: {}", dto.getNombre());
        // Necesitarías un método zonaService.crearZonaIndependiente(dto);
        // ZonaDTO nuevaZona = zonaService.crearZonaIndependiente(dto); 
        // return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success("Zona creada", nuevaZona));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(StandardResponse.error("Usar /api/v1/eventos/{idEvento}/zonas")); // Mejor deshabilitarlo
    }
    */

    // --- ACTUALIZAR ZONA (DEBE USAR DTO) ---
    @Operation(summary = "Actualizar zona", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zona actualizada", content = @Content(schema = @Schema(implementation = ZonaDTO.class))), // Devuelve DTO
            @ApiResponse(responseCode = "404", description = "Zona no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ZonaDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ZonaCreateDTO dto) { // Recibe CreateDTO (o un UpdateDTO si lo creas)
        log.info("PUT /api/v1/zonas/{}", id);
        // --- CORRECCIÓN DE LÓGICA ---
        // Necesitas un método en ZonaService que actualice usando el DTO
        // Ejemplo: ZonaDTO actualizada = zonaService.actualizarZona(id, dto);
        // --- FIN CORRECCIÓN ---
        // return ResponseEntity.ok(StandardResponse.success("Zona actualizada", actualizada));
        // Temporalmente retornamos error hasta implementar el service
        throw new UnsupportedOperationException("Actualizar zona aún no implementado con DTOs");
    }

    @Operation(summary = "Eliminar zona", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zona eliminada"),
            @ApiResponse(responseCode = "404", description = "Zona no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> eliminar(@PathVariable Integer id) {
        log.info("DELETE /api/v1/zonas/{}", id);
        // --- CORRECCIÓN DE MÉTODO ---
        zonaService.Eliminar(id); // Llama al método correcto
        // --- FIN CORRECCIÓN ---
        return ResponseEntity.ok(StandardResponse.success("Zona eliminada"));
    }
}