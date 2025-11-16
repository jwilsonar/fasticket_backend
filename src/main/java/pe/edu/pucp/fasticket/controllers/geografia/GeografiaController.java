package pe.edu.pucp.fasticket.controllers.geografia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.StandardResponse;
import pe.edu.pucp.fasticket.dto.geografia.DepartamentoDTO;
import pe.edu.pucp.fasticket.dto.geografia.DistritoDTO;
import pe.edu.pucp.fasticket.dto.geografia.ProvinciaDTO;
import pe.edu.pucp.fasticket.services.geografia.GeografiaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geografia")
@RequiredArgsConstructor
@Tag(name = "Geografía", description = "Endpoints para listas de Departamentos, Provincias y Distritos")
public class GeografiaController {
    private final GeografiaService geografiaService;

    @Operation(summary = "Listar todos los Departamentos (ej. Lima, Arequipa)")
    @GetMapping("/departamentos")
    public ResponseEntity<StandardResponse<List<DepartamentoDTO>>> getDepartamentos() {
        return ResponseEntity.ok(StandardResponse.success(
                "Departamentos obtenidos", geografiaService.listarDepartamentos()
        ));
    }

    @Operation(summary = "Listar Provincias (ciudades) de un Departamento")
    @GetMapping("/departamentos/{idDepa}/provincias")
    public ResponseEntity<StandardResponse<List<ProvinciaDTO>>> getProvincias(
            @PathVariable Integer idDepa) {
        return ResponseEntity.ok(StandardResponse.success(
                "Provincias obtenidas", geografiaService.listarProvinciasPorDepartamento(idDepa)
        ));
    }

    @Operation(summary = "Listar Distritos de una Provincia")
    @GetMapping("/provincias/{idProv}/distritos")
    public ResponseEntity<StandardResponse<List<DistritoDTO>>> getDistritos(
            @PathVariable Integer idProv) {
        return ResponseEntity.ok(StandardResponse.success(
                "Distritos obtenidos", geografiaService.listarDistritosPorProvincia(idProv)
        ));
    }
}
