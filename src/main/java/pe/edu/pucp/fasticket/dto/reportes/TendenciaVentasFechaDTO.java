package pe.edu.pucp.fasticket.dto.reportes;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class TendenciaVentasFechaDTO {
    private LocalDate fecha;
    private long ticketsVendidosDia;
    private Double ingresosNetosDia = 0.0;
}