package pe.edu.pucp.fasticket.controllers.fidelizacion;

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
import pe.edu.pucp.fasticket.dto.fidelizacion.*;
import pe.edu.pucp.fasticket.exception.ErrorResponse;
import pe.edu.pucp.fasticket.services.fidelizacion.FidelizacionService;

@Tag(
    name = "Fidelización - Administrador",
    description = "API para gestión administrativa de fidelización (reglas de puntos, códigos promocionales)."
)
@RestController
@RequestMapping("/api/v1/admin/fidelizacion")
@CrossOrigin(origins = {"http://localhost:4200", "https://fasticket.com"})
@RequiredArgsConstructor
@Slf4j
public class FidelizacionAdminController {

    private final FidelizacionService fidelizacionService;

    // ============ REGLAS DE PUNTOS ============

    @Operation(
        summary = "Listar todas las reglas de puntos",
        description = """
            Retorna todas las reglas de puntos configuradas en el sistema, incluyendo activas e inactivas.
            
            Las reglas de puntos definen:
            - Tipo de regla: CANJE (para canjear puntos) o COMPRA (para ganar puntos)
            - Soles por punto: Conversión de soles a puntos
            - Estado activo: Si la regla está en uso actualmente
            - Estado general: Estado adicional de la regla
            
            Ejemplo: Si solesPorPunto = 10, entonces 10 soles = 1 punto.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Reglas obtenidas exitosamente. Lista puede estar vacía si no hay reglas configuradas.",
            content = @Content(schema = @Schema(implementation = ReglaPuntosDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @GetMapping("/reglas-puntos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<ReglaPuntosDTO>>> listarReglasPuntos() {
        log.info("GET /api/v1/admin/fidelizacion/reglas-puntos");
        
        List<ReglaPuntosDTO> reglas = fidelizacionService.listarReglasPuntos();
        
        return ResponseEntity.ok(StandardResponse.success("Reglas obtenidas exitosamente.", reglas));
    }

    @Operation(
        summary = "Obtener una regla de puntos específica",
        description = """
            Retorna los detalles completos de una regla de puntos específica por su ID.
            
            La información incluye:
            - ID de la regla
            - Soles por punto (conversión)
            - Tipo de regla (CANJE o COMPRA)
            - Estado activo
            - Estado general
            - Lista de puntos generados asociados (si aplica)
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Regla obtenida exitosamente con todos sus detalles.",
            content = @Content(schema = @Schema(implementation = ReglaPuntosDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Regla de puntos no encontrada con el ID proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @GetMapping("/reglas-puntos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ReglaPuntosDTO>> obtenerReglaPuntos(
        @Parameter(description = "ID de la regla", required = true)
        @PathVariable Integer id) {

        log.info("GET /api/v1/admin/fidelizacion/reglas-puntos/{}", id);
        
        ReglaPuntosDTO regla = fidelizacionService.obtenerReglaPuntos(id);
        
        return ResponseEntity.ok(StandardResponse.success("Regla obtenida exitosamente.", regla));
    }

    @Operation(
        summary = "Crear una nueva regla de puntos",
        description = """
            Permite crear una nueva regla de puntos en el sistema.
            
            CAMPOS REQUERIDOS:
            - solesPorPunto: Cantidad de soles necesarios para obtener un punto (ej: 10.0 = 10 soles/punto)
            - tipoRegla: Tipo de regla (CANJE o COMPRA)
            - activo: Si la regla estará activa desde su creación
            - estado: Estado adicional de la regla
            
            CONSIDERACIONES:
            - Se recomienda tener solo una regla activa de cada tipo (CANJE y COMPRA)
            - El valor de solesPorPunto debe ser positivo
            - Una vez creada, la regla puede generar puntos asociados
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "Regla creada exitosamente. Retorna la regla creada con su ID asignado.",
            content = @Content(schema = @Schema(implementation = ReglaPuntosDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos inválidos o valores fuera de rango permitido.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @PostMapping("/reglas-puntos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ReglaPuntosDTO>> crearReglaPuntos(
        @Valid @RequestBody ReglaPuntosRequestDTO request) {

        log.info("POST /api/v1/admin/fidelizacion/reglas-puntos");
        
        ReglaPuntosDTO regla = fidelizacionService.crearReglaPuntos(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            StandardResponse.success("Regla creada exitosamente.", regla)
        );
    }

    @Operation(
        summary = "Actualizar una regla de puntos",
        description = """
            Permite actualizar una regla de puntos existente.
            
            CAMPOS ACTUALIZABLES:
            - solesPorPunto: Puede modificar la conversión de soles a puntos
            - tipoRegla: Puede cambiar el tipo (CANJE o COMPRA)
            - activo: Puede activar o desactivar la regla
            - estado: Puede modificar el estado general
            
            IMPORTANTE:
            - Al cambiar solesPorPunto, solo afectará a transacciones futuras
            - Los puntos ya generados con la regla anterior mantendrán su valor
            - Se recomienda crear una nueva regla en lugar de modificar una existente con mucho uso
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Regla actualizada exitosamente con los nuevos valores.",
            content = @Content(schema = @Schema(implementation = ReglaPuntosDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Regla de puntos no encontrada con el ID proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos inválidos o valores fuera de rango permitido.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @PutMapping("/reglas-puntos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<ReglaPuntosDTO>> actualizarReglaPuntos(
        @Parameter(description = "ID de la regla", required = true)
        @PathVariable Integer id,
        @Valid @RequestBody ReglaPuntosRequestDTO request) {

        log.info("PUT /api/v1/admin/fidelizacion/reglas-puntos/{}", id);
        
        ReglaPuntosDTO regla = fidelizacionService.actualizarReglaPuntos(id, request);
        
        return ResponseEntity.ok(StandardResponse.success("Regla actualizada exitosamente.", regla));
    }

    @Operation(
        summary = "Eliminar una regla de puntos",
        description = """
            Desactiva una regla de puntos existente en lugar de eliminarla físicamente.
            
            COMPORTAMIENTO:
            - La regla no se elimina de la base de datos, solo se marca como inactiva
            - Los puntos ya generados con esta regla no se modifican
            - La regla dejará de estar disponible para nuevas transacciones
            - Se puede reactivar luego usando el endpoint de actualización
            
            USO RECOMENDADO:
            - Usar para desactivar reglas temporales o reemplazadas
            - No afecta el historial de puntos existentes
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Regla desactivada exitosamente. Ya no estará disponible para nuevas transacciones."
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Regla de puntos no encontrada con el ID proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @DeleteMapping("/reglas-puntos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> eliminarReglaPuntos(
        @Parameter(description = "ID de la regla", required = true)
        @PathVariable Integer id) {

        log.info("DELETE /api/v1/admin/fidelizacion/reglas-puntos/{}", id);
        
        fidelizacionService.eliminarReglaPuntos(id);
        
        return ResponseEntity.ok(StandardResponse.success("Regla eliminada exitosamente.", null));
    }

    // ============ CÓDIGOS PROMOCIONALES ============

    @Operation(
        summary = "Listar todos los códigos promocionales",
        description = """
            Retorna todos los códigos promocionales configurados en el sistema.
            
            Incluye información sobre:
            - Código único del promocional
            - Descripción del descuento
            - Tipo de descuento (MONTO_FIJO o PORCENTAJE)
            - Valor del descuento
            - Fecha de expiración
            - Stock disponible
            - Cantidad permitida por cliente
            
            Los códigos pueden estar vigentes o expirados.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Códigos obtenidos exitosamente. Lista puede estar vacía si no hay códigos configurados.",
            content = @Content(schema = @Schema(implementation = CodigoPromocionalDTO.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @GetMapping("/codigos-promocionales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<List<CodigoPromocionalDTO>>> listarCodigosPromocionales() {
        log.info("GET /api/v1/admin/fidelizacion/codigos-promocionales");
        
        List<CodigoPromocionalDTO> codigos = fidelizacionService.listarCodigosPromocionales();
        
        return ResponseEntity.ok(StandardResponse.success("Códigos obtenidos exitosamente.", codigos));
    }

    @Operation(
        summary = "Obtener un código promocional específico",
        description = """
            Retorna los detalles completos de un código promocional por su ID.
            
            La información incluye:
            - ID del código promocional
            - Código único identificador
            - Descripción del descuento
            - Tipo (MONTO_FIJO o PORCENTAJE)
            - Valor del descuento
            - Fecha de expiración
            - Stock disponible
            - Cantidad permitida por cliente
            
            Útil para verificar el estado actual de un código específico.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Código obtenido exitosamente con todos sus detalles.",
            content = @Content(schema = @Schema(implementation = CodigoPromocionalDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Código promocional no encontrado con el ID proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @GetMapping("/codigos-promocionales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<CodigoPromocionalDTO>> obtenerCodigoPromocional(
        @Parameter(description = "ID del código", required = true)
        @PathVariable Integer id) {

        log.info("GET /api/v1/admin/fidelizacion/codigos-promocionales/{}", id);
        
        CodigoPromocionalDTO codigo = fidelizacionService.obtenerCodigoPromocional(id);
        
        return ResponseEntity.ok(StandardResponse.success("Código obtenido exitosamente.", codigo));
    }

    @Operation(
        summary = "Crear un nuevo código promocional",
        description = """
            Permite crear un nuevo código promocional en el sistema.
            
            CAMPOS REQUERIDOS:
            - codigo: Código único identificador (alfanumérico, único en el sistema)
            - descripcion: Descripción del descuento
            - tipo: Tipo de descuento (MONTO_FIJO o PORCENTAJE)
            - valor: Valor del descuento (en soles para MONTO_FIJO, porcentaje para PORCENTAJE)
            - fechaFin: Fecha y hora de expiración del código
            - stock: Cantidad de usos disponibles del código
            - cantidadPorCliente: Límite de usos por cliente (null = sin límite)
            
            VALIDACIONES:
            - El código debe ser único (no puede existir otro con el mismo código)
            - El stock debe ser mayor a 0
            - La fechaFin debe ser futura
            
            EJEMPLOS:
            - Porcentaje: 15% de descuento = {"tipo": "PORCENTAJE", "valor": 15}
            - Monto fijo: 50 soles de descuento = {"tipo": "MONTO_FIJO", "valor": 50}
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "Código creado exitosamente. Retorna el código con su ID asignado.",
            content = @Content(schema = @Schema(implementation = CodigoPromocionalDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos inválidos: código duplicado, stock inválido, fecha pasada, o valores fuera de rango.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @PostMapping("/codigos-promocionales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<CodigoPromocionalDTO>> crearCodigoPromocional(
        @Valid @RequestBody CodigoPromocionalRequestDTO request) {

        log.info("POST /api/v1/admin/fidelizacion/codigos-promocionales");
        
        CodigoPromocionalDTO codigo = fidelizacionService.crearCodigoPromocional(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            StandardResponse.success("Código creado exitosamente.", codigo)
        );
    }

    @Operation(
        summary = "Actualizar un código promocional",
        description = """
            Permite actualizar un código promocional existente.
            
            CAMPOS ACTUALIZABLES:
            - codigo: Puede cambiar el código (debe seguir siendo único)
            - descripcion: Puede modificar la descripción
            - tipo: Puede cambiar entre MONTO_FIJO y PORCENTAJE
            - valor: Puede modificar el valor del descuento
            - fechaFin: Puede extender o acortar la fecha de expiración
            - stock: Puede aumentar o reducir el stock disponible
            - cantidadPorCliente: Puede modificar el límite por cliente
            
            IMPORTANTE:
            - Si se cambia el código, debe verificar que no exista otro con ese código
            - Los cambios solo afectan usos futuros del código
            - Los usos ya realizados con la configuración anterior no se modifican
            - El stock no puede ser negativo
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Código actualizado exitosamente con los nuevos valores.",
            content = @Content(schema = @Schema(implementation = CodigoPromocionalDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Código promocional no encontrado con el ID proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos inválidos: código duplicado, stock inválido, o valores fuera de rango.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @PutMapping("/codigos-promocionales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<CodigoPromocionalDTO>> actualizarCodigoPromocional(
        @Parameter(description = "ID del código", required = true)
        @PathVariable Integer id,
        @Valid @RequestBody CodigoPromocionalRequestDTO request) {

        log.info("PUT /api/v1/admin/fidelizacion/codigos-promocionales/{}", id);
        
        CodigoPromocionalDTO codigo = fidelizacionService.actualizarCodigoPromocional(id, request);
        
        return ResponseEntity.ok(StandardResponse.success("Código actualizado exitosamente.", codigo));
    }

    @Operation(
        summary = "Eliminar un código promocional",
        description = """
            Elimina permanentemente un código promocional existente del sistema.
            
            COMPORTAMIENTO:
            - El código se elimina físicamente de la base de datos
            - No podrá ser usado por ningún cliente después de la eliminación
            - Los descuentos ya aplicados con este código se mantienen en el historial
            - No se puede recuperar después de eliminado
            
            USO RECOMENDADO:
            - Solo eliminar códigos que definitivamente ya no se utilizarán
            - Para desactivar temporalmente, usar el endpoint de actualizar y poner stock en 0
            - Verificar que el código no tenga usos pendientes antes de eliminar
            
            CUIDADO:
            - Esta operación es IRREVERSIBLE
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Código eliminado permanentemente del sistema."
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Código promocional no encontrado con el ID proporcionado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Token JWT no válido o ausente."),
        @ApiResponse(responseCode = "403", description = "Usuario no tiene rol ADMINISTRADOR.")
    })
    @DeleteMapping("/codigos-promocionales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<StandardResponse<Void>> eliminarCodigoPromocional(
        @Parameter(description = "ID del código", required = true)
        @PathVariable Integer id) {

        log.info("DELETE /api/v1/admin/fidelizacion/codigos-promocionales/{}", id);
        
        fidelizacionService.eliminarCodigoPromocional(id);
        
        return ResponseEntity.ok(StandardResponse.success("Código eliminado exitosamente.", null));
    }
}

