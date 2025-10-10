package pe.edu.pucp.fasticket.controllers.pago;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.fasticket.dto.pago.ComprobanteDTO;
import pe.edu.pucp.fasticket.dto.pago.PagoResumenDTO;
import pe.edu.pucp.fasticket.dto.pago.RegistrarPagoDTO;
import pe.edu.pucp.fasticket.services.pago.PagoServicio;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoServicio pagoServicio;

    public PagoController(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

    @PostMapping("/registrar")
    public ResponseEntity<ComprobanteDTO> registrarPago(@RequestBody RegistrarPagoDTO dto) {
        ComprobanteDTO comprobante = pagoServicio.registrarPagoFinal(dto);
        return ResponseEntity.ok(comprobante);
    }
}


