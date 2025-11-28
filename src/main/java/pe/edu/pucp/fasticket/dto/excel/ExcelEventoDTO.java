package pe.edu.pucp.fasticket.dto.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.Data;

@Data
public class ExcelEventoDTO {
    @ExcelCellName("Nombre Evento")
    private String nombre;

    @ExcelCellName("Descripcion")
    private String descripcion;

    @ExcelCellName("Fecha Inicio (YYYY-MM-DD)")
    private String fechaInicio;

    @ExcelCellName("Fecha Fin (YYYY-MM-DD)")
    private String fechaFin;

    @ExcelCellName("Hora Inicio (HH:mm)")
    private String horaInicio;

    @ExcelCellName("Hora Fin (HH:mm)")
    private String horaFin;

    @ExcelCellName("Aforo Disponible")
    private Integer aforoDisponible;

    @ExcelCellName("ID Local")
    private Integer idLocal;

    @ExcelCellName("Tipo Evento")
    private String tipoEvento;

    @ExcelCellName("Restricciones")
    private String restricciones;

    @ExcelCellName("Politicas Devolucion")
    private String politicasDevolucion;

    @ExcelCellName("Menores Permitidos")
    private Boolean menoresDeEdadPermitidos;

    @ExcelCellName("Imagen URL")
    private String imagenUrl;
}