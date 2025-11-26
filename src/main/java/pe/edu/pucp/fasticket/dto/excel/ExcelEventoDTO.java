package pe.edu.pucp.fasticket.dto.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.Data;

@Data
public class ExcelEventoDTO {
    @ExcelCellName("Nombre Evento")
    private String nombre;

    @ExcelCellName("Descripcion")
    private String descripcion;

    @ExcelCellName("Fecha (YYYY-MM-DD)")
    private String fechaEvento;

    @ExcelCellName("Hora Inicio (HH:mm)")
    private String horaInicio;

    @ExcelCellName("Hora Fin (HH:mm)")
    private String horaFin;

    @ExcelCellName("Aforo Disponible")
    private Integer aforoDisponible;

    @ExcelCellName("ID Local")
    private Integer idLocal;

    @ExcelCellName("Tipo Evento") // Debe coincidir con tu Enum (CONCIERTO, TEATRO, etc.)
    private String tipoEvento;

    @ExcelCellName("Restricciones")
    private String restricciones;

    @ExcelCellName("Politicas Devolucion")
    private String politicasDevolucion;

    @ExcelCellName("Menores Permitidos")
    private Boolean menoresDeEdadPermitidos;
}