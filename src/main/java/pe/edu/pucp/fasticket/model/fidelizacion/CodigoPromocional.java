package pe.edu.pucp.fasticket.model.fidelizacion;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un código promocional.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "CodigoPromocional")
public class CodigoPromocional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCodigoPromocional")
    private Integer idCodigoPromocional;

    @Column(name = "codigo", nullable = false, length = 100, unique = true)
    private String codigo;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fechaFin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 50)
    private TipoCodigoPromocional tipo;

    @Column(name = "valor")
    private Double valor;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "cantidadPorCliente")
    private Integer cantidadPorCliente;

    @Column(name = "activo", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean activo = true;
}

