package pe.edu.pucp.fasticket.controllers.compra;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.compra.CrearOrdenDTO;
import pe.edu.pucp.fasticket.dto.compra.OrdenResumenDTO;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.services.compra.OrdenServicio;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenServicio ordenServicio;

    public OrdenController(OrdenServicio ordenServicio) {
        this.ordenServicio = ordenServicio;
    }

    @PostMapping("/crear")
    public ResponseEntity<OrdenCompra> crearOrden(@RequestBody CrearOrdenDTO dto) {
        return ResponseEntity.ok(ordenServicio.crearOrden(dto));
    }

    @PostMapping("/resumen")
    public ResponseEntity<OrdenResumenDTO> generarResumen(@RequestBody CrearOrdenDTO dto) {
        return ResponseEntity.ok(ordenServicio.generarResumenOrden(dto));
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmarPago(@PathVariable Integer id) {
        ordenServicio.confirmarPagoOrden(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarOrden(@PathVariable Integer id) {
        ordenServicio.cancelarOrden(id);
        return ResponseEntity.ok().build();
    }
}