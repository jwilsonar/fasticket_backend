package pe.edu.pucp.fasticket.model.geografia;

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
@EqualsAndHashCode(exclude = {"departamento", "distritos"})
@ToString(exclude = {"departamento", "distritos"})
@Entity
@Table(name = "provincia")
public class Provincia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provincia")
    private Integer idProvincia;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idDepartamento", nullable = false)
    private Departamento departamento;

    @OneToMany(mappedBy = "provincia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Distrito> distritos = new ArrayList<>();
}
