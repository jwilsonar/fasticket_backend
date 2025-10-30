package pe.edu.pucp.fasticket.model.fidelizacion;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"puntosGenerados"})
@ToString(exclude = {"puntosGenerados"})
@Entity
@Table(name = "reglapuntos")
public class ReglaPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRegla")
    private Integer idRegla;

    @Column(name = "solesPorPunto")
    private Double solesPorPunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoRegla")
    private TipoRegla tipoRegla;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "estado")
    private Boolean estado;

    // --------- relaciones -----------

    @OneToMany(mappedBy = "reglaPuntos",cascade = {CascadeType.PERSIST, CascadeType.MERGE},orphanRemoval = false,fetch = FetchType.LAZY)
    private List<Puntos> puntosGenerados = new ArrayList<>();
}
