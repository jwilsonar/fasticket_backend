package pe.edu.pucp.fasticket.dto.pago;

import lombok.Data;
import java.time.LocalDate;
import pe.edu.pucp.fasticket.model.pago.EstadoPago;

@Data
public class PagoResumenDTO {
    private Integer idPago;
    private String metodo;
    private Double monto;
    private EstadoPago estado;
    private LocalDate fechaPago;
    private String nombreTitular;
    private String correo;

    public PagoResumenDTO(Integer idPago, String metodo, Double monto, EstadoPago estado, LocalDate fechaPago) {
        this.idPago = idPago;
        this.metodo = metodo;
        this.monto = monto;
        this.estado = estado;
        this.fechaPago = fechaPago;
    }
}
