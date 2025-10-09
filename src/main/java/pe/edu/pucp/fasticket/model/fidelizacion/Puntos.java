package pe.edu.pucp.fasticket.model.fidelizacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"reglaPuntos", "canjesDetalle"})
@ToString(exclude = {"reglaPuntos", "canjesDetalle"})
@Entity
@Table(name = "puntos")
public class Puntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPuntos")
    private Integer idPuntos;

    @Column(name = "puntosIniciales")
    private Integer puntosIniciales;

    @Column(name = "ganadoEn")
    private LocalDate ganadoEn;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoPuntos estado;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRegla")
    private ReglaPuntos reglaPuntos;

    @OneToMany(mappedBy = "puntos", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CanjeDetalle> canjesDetalle = new ArrayList<>();
}
