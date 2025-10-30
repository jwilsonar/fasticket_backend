package pe.edu.pucp.fasticket.dto.reportes;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResumenGeneralVentasDTO {
    private long ticketsVendidosTotal;
    private Double ingresosBrutosTotal = 0.0;
    private Double descuentosAplicadosTotal = 0.0;
    private Double ingresosNetosTotal = 0.0;
    private Double porcentajeOcupacion = 0.0;
}