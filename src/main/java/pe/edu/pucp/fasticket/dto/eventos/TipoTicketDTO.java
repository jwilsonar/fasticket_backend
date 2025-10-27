package pe.edu.pucp.fasticket.dto.eventos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer idEvento;
    private String nombreEvento;
    public TipoTicketDTO(TipoTicket p_tipoTicket){
        this.idTipoTicket = p_tipoTicket.getIdTipoTicket();
        this.stock = p_tipoTicket.getStock();
        this.cantidadDisponible = p_tipoTicket.getCantidadDisponible();
        this.cantidadVendida = p_tipoTicket.getCantidadVendida();
        this.nombre = p_tipoTicket.getNombre();
        this.descripcion = p_tipoTicket.getDescripcion();
        this.precio = p_tipoTicket.getPrecio();
        this.fechaInicioVenta = p_tipoTicket.getFechaInicioVenta();
        this.fechaFinVenta = p_tipoTicket.getFechaFinVenta();
        this.activo = p_tipoTicket.getActivo();

        if (p_tipoTicket.getEvento() != null) {
            this.idEvento = p_tipoTicket.getEvento().getIdEvento();
            this.nombreEvento = p_tipoTicket.getEvento().getNombre();
        }
    }
}
