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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"canjes", "puntosGenerados"})
@ToString(exclude = {"canjes", "puntosGenerados"})
@Entity
@Table(name = "reglapuntos")
public class ReglaPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegla")
    private Integer idRegla;

    @Column(name = "solesPorPunto")
    private Double solesPorPunto;

    @Column(name = "puntosPorBloque")
    private Integer puntosPorBloque;

    @Column(name = "descuentoPorBloque")
    private Double descuentoPorBloque;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "estado", length = 255)
    private String estado;

    @Column(name = "fechaInicioVigencia")
    private LocalDate fechaInicioVigencia;

    @Column(name = "diasVigencia")
    private Integer diasVigencia;

    @OneToMany(mappedBy = "reglaPuntos", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Canje> canjes = new ArrayList<>();

    @OneToMany(mappedBy = "reglaPuntos", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Puntos> puntosGenerados = new ArrayList<>();
}
