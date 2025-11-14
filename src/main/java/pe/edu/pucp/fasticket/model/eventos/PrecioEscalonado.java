package pe.edu.pucp.fasticket.model.eventos;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "precio_escalonado")
@EqualsAndHashCode(exclude = {"tipoTicket"})
@ToString(exclude = {"tipoTicket"})
public class PrecioEscalonado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPrecio;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre_etapa", nullable = false)
    private Etapa nombreEtapa;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activo", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_ticket", nullable = false)
    private TipoTicket tipoTicket;
}