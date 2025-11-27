package pe.edu.pucp.fasticket.controllers.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.services.excel.CargaMasivaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

@Tag(
        name = "Carga Masiva (Admin)",
        description = "API para la importación masiva de datos maestros (Locales, Eventos) desde archivos Excel. " +
                "Estas operaciones son exclusivas para usuarios con rol de ADMINISTRADOR."
)
@RestController
@RequestMapping("/api/v1/admin/carga-masiva")
@RequiredArgsConstructor
@Slf4j
public class CargaMasivaController {

    private final CargaMasivaService cargaMasivaService;

    @Operation(
            summary = "Cargar Locales desde Excel",
            description = "Procesa un archivo Excel (.xlsx) para crear locales masivamente. " +
                    "El archivo debe tener las columnas: Nombre, Direccion, Aforo Total, URL Mapa, Imagen URL, ID Distrito.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Carga procesada exitosamente",
                    content = @Content(schema = @Schema(implementation = StandardResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el formato del archivo o datos inválidos",
                    content = @Content(schema = @Schema(implementation = StandardResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol ADMINISTRADOR)")
    })
    @PostMapping(value = "/locales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirLocales(
            @Parameter(description = "Archivo Excel (.xlsx) con la data de locales", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("POST /api/v1/admin/carga-masiva/locales - Archivo: {}", file.getOriginalFilename());
        try {
            String mensaje = cargaMasivaService.cargarLocales(file);
            return ResponseEntity.ok(StandardResponse.success(mensaje, null));
        } catch (Exception e) {
            log.error("Error en carga masiva de locales: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new StandardResponse<>(false, e.getMessage(), null));
        }
    }

    @Operation(
            summary = "Cargar Eventos desde Excel",
            description = "Procesa un archivo Excel (.xlsx) para crear eventos masivamente. " +
                    "Los eventos se crean en estado BORRADOR. " +
                    "Columnas requeridas: Nombre Evento, Descripcion, Fecha, Hora Inicio, Hora Fin, Aforo Disponible, ID Local, Tipo Evento, Restricciones, Politicas Devolucion, Menores Permitidos.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Carga procesada exitosamente",
                    content = @Content(schema = @Schema(implementation = StandardResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error en el formato del archivo o datos inválidos",
                    content = @Content(schema = @Schema(implementation = StandardResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol ADMINISTRADOR)")
    })
    @PostMapping(value = "/eventos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<String>> subirEventos(
            @Parameter(description = "Archivo Excel (.xlsx) con la data de eventos", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("POST /api/v1/admin/carga-masiva/eventos - Archivo: {}", file.getOriginalFilename());
        try {
            String mensaje = cargaMasivaService.cargarEventos(file);
            return ResponseEntity.ok(StandardResponse.success(mensaje, null));
        } catch (Exception e) {
            log.error("Error en carga masiva de eventos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new StandardResponse<>(false, e.getMessage(), null));
        }
    }
}
