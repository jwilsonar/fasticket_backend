package pe.edu.pucp.fasticket.dto.compra;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.repository.eventos.TipoTicketRepositorio;

@Data
@NoArgsConstructor
@Schema(description = "DTO de resumen de una orden de compra")
public class OrdenResumenDTO {
    
    @Schema(description = "ID de la orden", example = "1")
    private Integer idOrden;
    
    @Schema(description = "Nombre del evento asociado", example = "Concierto de Rock")
    private String nombreEvento;
    
    @Schema(description = "Fecha del evento", example = "2024-12-25")
    private LocalDate fecha;
    
    @Schema(description = "Hora de inicio del evento", example = "20:00:00")
    private LocalTime hora;
    
    @Schema(description = "Estado de la orden", example = "PENDIENTE", allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO", "ANULADO"})
    private String estado;
    
    @Schema(description = "Nombre del local donde se realiza el evento", example = "Estadio Nacional")
    private String nombreLocal;
    
    @Schema(description = "Lista de items de la orden")
    private List<ItemResumenDTO> items;
    
    @Schema(description = "Subtotal de la orden (sin descuentos ni IGV)", example = "100.00")
    private Double subtotal;
    
    @Schema(description = "IGV aplicado", example = "18.00")
    private Double igv;
    
    @Schema(description = "Descuento por membresía aplicado", example = "10.00")
    private Double descuentoPorMembresia;
    
    @Schema(description = "Descuento por canje aplicado", example = "0.00")
    private Double descuentoPorCanje;
    
    @Schema(description = "Descuento promocional aplicado", example = "5.00")
    private Double descuentoPromocional;
    
    @Schema(description = "Código promocional aplicado", example = "DESC10")
    private String codigoPromocional;
    
    @Schema(description = "Total final de la orden", example = "103.00")
    private Double total;
    
    @Schema(description = "Código de seguimiento de la orden", example = "ORD-2024-001")
    private String codigoSeguimiento;
    
    @Schema(description = "Método de pago utilizado", example = "TARJETA")
    private String metodoPago;
    
    @Schema(description = "Fecha de creación de la orden", example = "2024-12-20")
    private LocalDate fechaOrden;
    
    @Schema(description = "Fecha de expiración de la reserva", example = "2024-12-20T15:30:00")
    private LocalDateTime fechaExpiracion;
    
    @Schema(description = "ID del cliente que realizó la orden", example = "1")
    private Integer idCliente;
    
    @Schema(description = "Email del cliente", example = "cliente@example.com")
    private String emailCliente;

    public OrdenResumenDTO(OrdenCompra orden, TipoTicketRepositorio tipoTicketRepositorio) {
        this.idOrden = orden.getIdOrdenCompra();
        this.fechaOrden = orden.getFechaOrden();
        this.total = orden.getTotal();
        this.subtotal = orden.getSubtotal();
        this.igv = orden.getIgv();
        this.descuentoPorMembresia = orden.getDescuentoPorMembrecia();
        this.descuentoPorCanje = orden.getDescuentoPorCanje();
        this.descuentoPromocional = orden.getDescuentoPromocional();
        this.codigoPromocional = orden.getCodigoPromocionalAplicado();
        this.estado = orden.getEstado().toString();
        this.codigoSeguimiento = orden.getCodigoSeguimiento();
        this.metodoPago = orden.getMetodoPago();
        this.fechaExpiracion = orden.getFechaExpiracion();
        
        if (orden.getCliente() != null) {
            this.idCliente = orden.getCliente().getIdPersona();
            this.emailCliente = orden.getCliente().getEmail();
        }
        
        if (orden.getItems() != null && !orden.getItems().isEmpty()) {
            // Obtener evento a través del repositorio
            Evento evento = tipoTicketRepositorio.findEventoByTipoTicket(orden.getItems().get(0).getTipoTicket().getIdTipoTicket())
                    .orElse(null);

            if (evento != null) {
                this.nombreEvento = evento.getNombre();
                this.fecha = evento.getFechaEvento();
                this.hora = evento.getHoraInicio();
                if (evento.getLocal() != null) {
                    this.nombreLocal = evento.getLocal().getNombre();
                }
            }

            this.items = orden.getItems().stream().map(item -> {
                ItemResumenDTO itemDTO = new ItemResumenDTO();
                itemDTO.setCantidad(item.getCantidad());
                itemDTO.setPrecioUnitario(item.getPrecio());
                itemDTO.setNombreTipoTicket(item.getTipoTicket().getNombre());
                return itemDTO;
            }).collect(Collectors.toList());
        }
    }

    public OrdenResumenDTO(OrdenCompra orden) {
        this.idOrden = orden.getIdOrdenCompra();
        this.fechaOrden = orden.getFechaOrden();
        this.total = orden.getTotal();
        this.subtotal = orden.getSubtotal();
        this.igv = orden.getIgv();
        this.descuentoPorMembresia = orden.getDescuentoPorMembrecia();
        this.descuentoPorCanje = orden.getDescuentoPorCanje();
        this.descuentoPromocional = orden.getDescuentoPromocional();
        this.codigoPromocional = orden.getCodigoPromocionalAplicado();
        this.estado = orden.getEstado().toString();
        this.codigoSeguimiento = orden.getCodigoSeguimiento();
        this.metodoPago = orden.getMetodoPago();
        this.fechaExpiracion = orden.getFechaExpiracion();
        
        if (orden.getCliente() != null) {
            this.idCliente = orden.getCliente().getIdPersona();
            this.emailCliente = orden.getCliente().getEmail();
        }
        
        if (orden.getItems() != null && !orden.getItems().isEmpty()) {
            Evento evento = orden.getItems().get(0).getTipoTicket().getEvento();
            if (evento != null) {
                this.nombreEvento = evento.getNombre();
                this.fecha = evento.getFechaEvento();
                this.hora = evento.getHoraInicio();
                if (evento.getLocal() != null) {
                    this.nombreLocal = evento.getLocal().getNombre();
                }
            }
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
