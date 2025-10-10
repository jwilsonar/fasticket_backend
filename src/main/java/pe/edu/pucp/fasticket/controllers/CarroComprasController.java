package pe.edu.pucp.fasticket.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.AddItemRequestDTO;
import pe.edu.pucp.fasticket.dto.CarroComprasDTO;
import pe.edu.pucp.fasticket.services.CarroComprasService;

@RestController
@RequestMapping("/api/carrito") // Ruta base para todos los endpoints del carrito
@CrossOrigin(origins = "http://localhost:4200") // Permite peticiones desde el frontend en Angular/React
@RequiredArgsConstructor
public class CarroComprasController {

    private final CarroComprasService carroComprasService;

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<CarroComprasDTO> verCarrito(@PathVariable Integer idCliente) {
        CarroComprasDTO carrito = carroComprasService.verCarrito(idCliente);
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/items")
    public ResponseEntity<CarroComprasDTO> agregarItem(@RequestBody AddItemRequestDTO request) {
        CarroComprasDTO carritoActualizado = carroComprasService.agregarItemAlCarrito(request);
        return ResponseEntity.ok(carritoActualizado);
    }

    @DeleteMapping("/items/{idItemCarrito}")
    public ResponseEntity<CarroComprasDTO> eliminarItem(
            @PathVariable Integer idItemCarrito,
            @RequestParam Integer idCliente) {
        CarroComprasDTO carritoActualizado = carroComprasService.eliminarItemDelCarrito(idItemCarrito, idCliente);
        return ResponseEntity.ok(carritoActualizado);
    }
}