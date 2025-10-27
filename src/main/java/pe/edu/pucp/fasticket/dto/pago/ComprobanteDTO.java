package pe.edu.pucp.fasticket.dto.pago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;
import pe.edu.pucp.fasticket.dto.compra.ItemResumenDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComprobanteDTO {
    private String numeroSerie;
    private String codigoCompra;
    private String nombreEvento;
    private String nombreLocal;
    private LocalDate fechaEvento;
    private LocalTime horaEvento;
    private LocalDate fechaCompra;
    private LocalTime horaCompra;
    private int cantidadEntradas;
    private List<ItemResumenDTO> items;
    private List<DatosAsistenteDTO> asistentes;
    private Double montoTotal;
    private String detallePago;
    private String numeroTarjeta;
    private String estadoCompra;
    private LocalDateTime fechaEmision;
}

