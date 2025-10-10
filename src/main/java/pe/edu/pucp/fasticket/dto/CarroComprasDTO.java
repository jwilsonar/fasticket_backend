package pe.edu.pucp.fasticket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CarroComprasDTO {
    private Integer idCarro;
    private List<ItemCarritoDTO> items;
    private Double subtotal;
    private Double total;
}