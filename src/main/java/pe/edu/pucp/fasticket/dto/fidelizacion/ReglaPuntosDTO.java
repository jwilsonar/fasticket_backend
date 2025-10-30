package pe.edu.pucp.fasticket.dto.fidelizacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.fidelizacion.ReglaPuntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoRegla;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReglaPuntosDTO {
    private Integer idRegla;
    private Double solesPorPunto;
    private TipoRegla tipoRegla;
    private Boolean activo;
    private Boolean estado;

    public ReglaPuntosDTO(ReglaPuntos regla) {
        this.idRegla = regla.getIdRegla();
        this.solesPorPunto = regla.getSolesPorPunto();
        this.tipoRegla = regla.getTipoRegla();
        this.activo = regla.getActivo();
        this.estado = regla.getEstado();
    }
}

