package pe.edu.pucp.fasticket.dto.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutCarritoRequestDTO {

    @NotEmpty(message = "La lista de items no puede estar vacía")
    @Valid
    private List<AsistenteParaItemDTO> itemsConAsistentes;

    // --- INICIO RF-081 (Opcional) ---
    @Schema(description = "RUC para facturación (Opcional)", example = "20100053453")
    private String ruc;

    @Schema(description = "Razón Social para facturación (Opcional)", example = "Empresa S.A.C.")
    private String razonSocial;

    @Schema(description = "Dirección Fiscal para facturación (Opcional)", example = "Av. Siempre Viva 123")
    private String direccionFiscal;
    // --- FIN RF-081 ---
}
