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
@EqualsAndHashCode(exclude = {"beneficios", "clientesMembresia"})
@ToString(exclude = {"beneficios", "clientesMembresia"})
@Entity
@Table(name = "membresia")
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_membresia")
    private Integer idMembresia;

    @Column(name = "tipo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoMembresia tipo;

    @Column(name = "min_entradas", nullable = false)
    private Integer minEntradas;

    @Column(name = "max_entradas")
    private Integer maxEntradas;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "activo")
    private Boolean activo = true;

    @OneToMany(mappedBy = "membresia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Beneficio> beneficios = new ArrayList<>();

    @OneToMany(mappedBy = "membresia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ClienteMembresia> clientesMembresia = new ArrayList<>();
}
