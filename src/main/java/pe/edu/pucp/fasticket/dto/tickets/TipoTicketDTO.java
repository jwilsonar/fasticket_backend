package pe.edu.pucp.fasticket.dto.tickets;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.eventos.TipoTicket;
import pe.edu.pucp.fasticket.model.fidelizacion.Promocion;

@Data
@NoArgsConstructor
public class TipoTicketDTO {
    private Integer idTipoTicket, stock, cantidadDisponible, cantidadVendida;
    private String nombre, descripcion;
    private Double precio;
    private LocalDateTime fechaInicioVenta, fechaFinVenta;
    private Boolean activo;
    private Evento evento;
    private List<Ticket> tickets;
    private List<Promocion> promocionesAplicables;

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
        this.evento = p_tipoTicket.getEvento();
        this.tickets = p_tipoTicket.getTickets();
        this.promocionesAplicables = p_tipoTicket.getPromocionesAplicables();
    }
}
