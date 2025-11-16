package pe.edu.pucp.fasticket.dto.compra;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO para crear una nueva orden de compra")
public class CrearOrdenDTO {
    
    @Schema(description = "ID del cliente que realiza la compra", example = "1", required = true)
    private Integer idCliente;
    
    @Schema(description = "Lista de items seleccionados para la compra", required = true)
    private List<ItemSeleccionadoDTO> items;

    // --- INICIO RF-081 (Opcional) ---
    @Schema(description = "RUC para facturación (Opcional)", example = "20100053453")
    private String ruc;

    @Schema(description = "Razón Social para facturación (Opcional)", example = "Empresa S.A.C.")
    private String razonSocial;

    @Schema(description = "Dirección Fiscal para facturación (Opcional)", example = "Av. Siempre Viva 123")
    private String direccionFiscal;
    // --- FIN RF-081 ---
}
