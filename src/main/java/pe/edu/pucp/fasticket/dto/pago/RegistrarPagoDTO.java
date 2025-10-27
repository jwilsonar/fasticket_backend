package pe.edu.pucp.fasticket.dto.pago;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistrarPagoDTO {
    private Integer idOrden;
    private String nombreTitular;
    private String correo;
    private String numeroTarjeta;
    private String fechaCaducidad;
    private String cvv;
    private Integer numeroCuotas;
    private Double monto;
    private Integer idUsuario;
}