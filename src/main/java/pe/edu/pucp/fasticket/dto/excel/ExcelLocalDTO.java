package pe.edu.pucp.fasticket.dto.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.Data;

@Data
public class ExcelLocalDTO {
    @ExcelCellName("Nombre")
    private String nombre;

    @ExcelCellName("Direccion")
    private String direccion;

    @ExcelCellName("Aforo Total")
    private Integer aforoTotal;

    @ExcelCellName("URL Mapa")
    private String urlMapa;

    @ExcelCellName("ID Distrito")
    private Integer idDistrito;
}