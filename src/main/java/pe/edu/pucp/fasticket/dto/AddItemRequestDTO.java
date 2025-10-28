package pe.edu.pucp.fasticket.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.dto.compra.DatosAsistenteDTO;

@Data
@NoArgsConstructor
@Schema(description = "DTO para agregar un item al carrito de compras")
public class AddItemRequestDTO {
    
    @Schema(description = "ID del tipo de ticket a agregar", example = "1", required = true)
    private Integer idTipoTicket;
    
    @Schema(description = "Cantidad de tickets a agregar", example = "2", required = true, minimum = "1")
    private Integer cantidad;
    
    @Schema(description = "ID del cliente que agrega el item", example = "1", required = true)
    private Integer idCliente;
    
    @Schema(description = "Lista de datos de asistentes para cada ticket", required = true)
    @Valid
    @NotEmpty
    private List<DatosAsistenteDTO> asistentes;
}