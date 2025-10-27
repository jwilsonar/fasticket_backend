package pe.edu.pucp.fasticket.dto.compra;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class OrdenResumenDTO {
    private Integer idOrden;
    private String nombreEvento;
    private LocalDate fecha;
    private LocalTime hora;
    private String estado;
    private String nombreLocal;
    private List<ItemResumenDTO> items;
    private double subtotal;
    private double total;

    public OrdenResumenDTO(OrdenCompra orden) {
        this.idOrden = orden.getIdOrdenCompra();
        this.fecha = orden.getFechaOrden();
        this.total = orden.getTotal();
        this.estado = orden.getEstado().toString();
        if (orden.getItems() != null && !orden.getItems().isEmpty()) {
            Evento evento = orden.getItems().get(0).getTipoTicket().getEvento();

            this.nombreEvento = evento.getNombre();
            this.fecha = evento.getFechaEvento();
            this.hora = evento.getHoraInicio();
            this.nombreLocal = evento.getLocal().getNombre();

            this.items = orden.getItems().stream().map(item -> {
                ItemResumenDTO itemDTO = new ItemResumenDTO();
                itemDTO.setCantidad(item.getCantidad());
                itemDTO.setPrecioUnitario(item.getPrecio());
                itemDTO.setNombreTipoTicket(item.getTipoTicket().getNombre());
                return itemDTO;
            }).collect(Collectors.toList());
        }
    }
}
