package pe.edu.pucp.fasticket.dto.pago;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistrarPagoDTO {
    private Integer idOrden;
    private String nombreTitular;
    private String correo;

    @Pattern(
            regexp = "^\\d{16}$",
            message = "El número de tarjeta debe tener exactamente 16 dígitos"
    )
    private String numeroTarjeta;

    @Future
    private LocalDate fechaCaducidad;

    @Pattern(
            regexp = "^\\d{3}$",
            message = "El CVV debe tener exactamente 3 dígitos"
    )
    private String cvv;
    private Integer numeroCuotas;
    private Double monto;
    private Integer idUsuario;
    private String ruc;
    private String razonSocial;
    private String direccionFiscal;
}