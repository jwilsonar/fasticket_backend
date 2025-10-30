package pe.edu.pucp.fasticket.dto.reportes;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ReporteVentasEventoDTO {
    private ReporteInfoDTO reporteInfo;
    private EventoDetallesDTO eventoDetalles;
    private ResumenGeneralVentasDTO resumenGeneralVentas;
    private List<DesgloseCategoriaTicketDTO> desglosePorCategoriaTicket;
    private List<TendenciaVentasFechaDTO> tendenciaVentasPorFecha;
}