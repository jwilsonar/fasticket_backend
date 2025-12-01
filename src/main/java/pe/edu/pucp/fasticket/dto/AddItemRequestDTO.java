package pe.edu.pucp.fasticket.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;

@Data
@NoArgsConstructor
@Schema(description = "DTO para agregar un item al carrito de compras")
public class AddItemRequestDTO {

    @Schema(description = "ID del cliente que agrega el item", example = "1", required = true)
    @NotNull(message = "El ID del cliente es requerido")
    private Integer idCliente;

    @Schema(description = "ID del tipo de ticket a agregar (formato simple)", example = "12")
    private Integer idTipoTicket;

    @Schema(description = "Cantidad de tickets a agregar (formato simple)", example = "2", minimum = "1")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @Schema(description = "Lista de items a agregar (formato múltiple)")
    @Valid
    private List<ItemRequest> items;

    @Data
    @Schema(description = "Item individual para agregar al carrito")
    public static class ItemRequest {

        @Schema(description = "ID del tipo de ticket", example = "12", required = true)
        @NotNull(message = "El ID del tipo de ticket es requerido")
        private Integer idTipoTicket;

        @Schema(description = "Cantidad de tickets", example = "2", minimum = "1", required = true)
        @NotNull(message = "La cantidad es requerida")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;
    }

    public boolean esFormatoMultiple() {
        return items != null && !items.isEmpty();
    }

    public List<ItemRequest> getItemsNormalizados() {
        if (esFormatoMultiple()) {
            return items;
        }

        if (idTipoTicket != null && cantidad != null) {
            ItemRequest item = new ItemRequest();
            item.setIdTipoTicket(idTipoTicket);
            item.setCantidad(cantidad);
            return List.of(item);
        }

        throw new IllegalArgumentException(
                "Debes proporcionar items usando el formato simple (idTipoTicket + cantidad) " +
                        "o el formato múltiple (array de items)"
        );
    }
}