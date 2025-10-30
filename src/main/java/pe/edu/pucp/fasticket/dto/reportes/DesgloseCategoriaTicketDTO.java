package pe.edu.pucp.fasticket.dto.reportes;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DesgloseCategoriaTicketDTO {
    private String categoriaNombre; // Nombre de Zona o TipoTicket
    private Double precioUnitarioBase = 0.0; // Precio original
    private Integer ticketsDisponibles = 0; // Stock inicial (si lo tienes)
    private long ticketsVendidos = 0;
    private Double ingresosBrutosCategoria = 0.0;
    private Double descuentosCategoria = 0.0; // Descuentos específicos a esta categoría
    private Double ingresosNetosCategoria = 0.0;
    private Double porcentajeVentasCategoria = 0.0; // Opcional
}