package pe.edu.pucp.fasticket.dto.compra;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO los items procesados en una orden de compra")
public class ItemsDTO {
    @Schema(description = "ID del item en el carrito de compras", example = "1")
    private Integer idTipoTicket;
    @Schema(description = "Cantidad de tickets para el item", example = "2")
    private Integer cantidad;

    public ItemsDTO(Integer idTipoTicket, Integer cantidad) {
        this.idTipoTicket = idTipoTicket;
        this.cantidad = cantidad;
    }
}
