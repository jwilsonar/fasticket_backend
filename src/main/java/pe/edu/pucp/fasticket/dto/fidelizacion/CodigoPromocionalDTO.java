package pe.edu.pucp.fasticket.dto.fidelizacion;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.fasticket.model.fidelizacion.CodigoPromocional;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoCodigoPromocional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoPromocionalDTO {
    private Integer idCodigoPromocional;
    private String codigo;
    private String descripcion;
    private LocalDateTime fechaFin;
    private TipoCodigoPromocional tipo;
    private Double valor;
    private Integer stock;
    private Integer cantidadPorCliente;

    public CodigoPromocionalDTO(CodigoPromocional codigoPromocional) {
        this.idCodigoPromocional = codigoPromocional.getIdCodigoPromocional();
        this.codigo = codigoPromocional.getCodigo();
        this.descripcion = codigoPromocional.getDescripcion();
        this.fechaFin = codigoPromocional.getFechaFin();
        this.tipo = codigoPromocional.getTipo();
        this.valor = codigoPromocional.getValor();
        this.stock = codigoPromocional.getStock();
        this.cantidadPorCliente = codigoPromocional.getCantidadPorCliente();
    }
}

