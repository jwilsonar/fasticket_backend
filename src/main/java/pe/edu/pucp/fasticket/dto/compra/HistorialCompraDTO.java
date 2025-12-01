package pe.edu.pucp.fasticket.dto.compra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.compra.EstadoCompra;

@Schema(description = "DTO para historial de compras del cliente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCompraDTO {

    private Integer idOrden;
    private String codigoCompra;
    private String estado;
    private LocalDateTime fechaCompra;
    private LocalDateTime fechaEvento;

    private String nombreEvento;
    private String lugarEvento;
    private String direccionLocal;
    private String imagenUrl;

    private Double subtotal;
    private Double descuentoPuntos;
    private Double descuentoCupon;
    private Double totalPagado;

    private Integer puntosGanados;
    private Integer puntosCanjeados;

    private String medioPago;
    private String numeroTarjeta;
    private Integer idTransaccionPago;
    private String estadoPago;

    private List<DetalleItemDTO> items;

    private Boolean tieneComprobante;
    private String codigoSeguimiento;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleItemDTO {
        private String nombreTipoTicket;
        private Integer cantidad;
        private Double precioUnitario;
        private Double subtotalLinea;
        private List<DetalleAsistenteDTO> asistentes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleAsistenteDTO {
        private Integer idTicket;
        private String nombreCompleto;
        private String documento;
        private String codigoQr;
    }
}

