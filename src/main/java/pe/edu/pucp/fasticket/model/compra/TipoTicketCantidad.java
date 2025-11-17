package pe.edu.pucp.fasticket.model.compra;

 // Clase interna para representar la dupla (idTipoTicket, cantidad)
public class TipoTicketCantidad {
    private Integer idTipoTicket;
    private Integer cantidad;

    public TipoTicketCantidad() {}

    public TipoTicketCantidad(Integer idTipoTicket, Integer cantidad) {
        this.idTipoTicket = idTipoTicket;
        this.cantidad = cantidad;
    }

    public Integer getIdTipoTicket() {
        return idTipoTicket;
    }
    
    @Override
    public String toString() {
        return "TipoTicketCantidad{idTipoTicket=" + idTipoTicket + ", cantidad=" + cantidad + "}";
    }
}