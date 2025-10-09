package pe.edu.pucp.fasticket.model.fidelizacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@EqualsAndHashCode(exclude = {"reglaPuntos", "detalles"})
@ToString(exclude = {"reglaPuntos", "detalles"})
@Entity
@Table(name = "canje")
public class Canje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCanje")
    private Integer idCanje;

    @Column(name = "fechaCanje")
    private LocalDate fechaCanje;

    @Column(name = "puntosSolicitados")
    private Integer puntosSolicitados;

    @Column(name = "puntosConsumidos")
    private Integer puntosConsumidos;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idReglapuntos")
    private ReglaPuntos reglaPuntos;

    @OneToMany(mappedBy = "canje", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CanjeDetalle> detalles = new ArrayList<>();
}
