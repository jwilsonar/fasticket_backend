package pe.edu.pucp.fasticket.dto.eventos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoTicketDTO {
    
    private Integer idTipoTicket;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer cantidadDisponible;
    private Integer cantidadVendida;
    private Integer idEvento;
    private String nombreEvento;
    private Integer idZona;
    private String nombreZona;
    private Boolean activo;
}
