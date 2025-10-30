package pe.edu.pucp.fasticket.dto.fidelizacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.fidelizacion.Puntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoTransaccion;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntosDTO {
    private Integer idPuntos;
    private Integer cantPuntos;
    private LocalDate fechaVencimiento;
    private LocalDate fechaTransaccion;
    private TipoTransaccion tipoTransaccion;
    private Boolean activo;
    private Integer idRegla;
    private Integer idCliente;

    public PuntosDTO(Puntos puntos) {
        this.idPuntos = puntos.getIdPuntos();
        this.cantPuntos = puntos.getCantPuntos();
        this.fechaVencimiento = puntos.getFechaVencimiento();
        this.fechaTransaccion = puntos.getFechaTransaccion();
        this.tipoTransaccion = puntos.getTipoTransaccion();
        this.activo = puntos.getActivo();
        this.idRegla = puntos.getReglaPuntos() != null ? puntos.getReglaPuntos().getIdRegla() : null;
        this.idCliente = puntos.getCliente() != null ? puntos.getCliente().getIdPersona() : null;
    }
}

