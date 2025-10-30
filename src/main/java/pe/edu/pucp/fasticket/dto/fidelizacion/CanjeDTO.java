package pe.edu.pucp.fasticket.dto.fidelizacion;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.fidelizacion.Canje;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanjeDTO {
    private Integer idCanje;
    private LocalDate fechaCanje;
    private Integer idOrdenCompra;
    private Integer idPuntos;

    public CanjeDTO(Canje canje) {
        this.idCanje = canje.getIdCanje();
        this.fechaCanje = canje.getFechaCanje();
        this.idOrdenCompra = canje.getOrdenCompra() != null ? canje.getOrdenCompra().getIdOrdenCompra() : null;
        this.idPuntos = canje.getPuntos() != null ? canje.getPuntos().getIdPuntos() : null;
    }
}

