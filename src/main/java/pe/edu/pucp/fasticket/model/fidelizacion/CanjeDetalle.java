package pe.edu.pucp.fasticket.model.fidelizacion;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"puntos", "canje"})
@ToString(exclude = {"puntos", "canje"})
@IdClass(CanjeDetalle.CanjeDetalleId.class)
@Entity
@Table(name = "canjedetalle")
public class CanjeDetalle {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPuntos")
    private Puntos puntos;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCanje")
    private Canje canje;

    @Column(name = "puntosUsados")
    private Integer puntosUsados;

    @Data
    @NoArgsConstructor
    public static class CanjeDetalleId implements Serializable {
        private Integer puntos;
        private Integer canje;
    }
}
