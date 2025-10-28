package pe.edu.pucp.fasticket.model.eventos;

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
import pe.edu.pucp.fasticket.model.geografia.Distrito;

/**
 * Entidad que representa un local donde se realizan eventos.
 * RF-001: El sistema deberá permitir crear locales con nombre, dirección, distrito, aforo total, geolocalización y estado (activo/inactivo).
 * RF-002: El sistema deberá permitir editar la información de un local existente incluyendo el aforo máximo por cada zona del local.
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"distrito", "zonas"})
@ToString(exclude = {"distrito", "zonas"})
@Entity
@Table(name = "Local")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLocal")
    private Integer idLocal;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "urlMapa")
    private String urlMapa;

    @Column(name = "direccion", length = 300)
    private String direccion;

    /**
     * Aforo total del local.
     * RF-003: La suma de aforos por zona no debe exceder este valor.
     */
    @Column(name = "aforoTotal", nullable = false)
    private Integer aforoTotal;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "usuarioCreacion")
    private Integer usuarioCreacion;

    @Column(name = "fechaCreacion")
    private java.time.LocalDate fechaCreacion;

    @Column(name = "usuarioActualizacion")
    private Integer usuarioActualizacion;

    @Column(name = "fechaActualizacion")
    private java.time.LocalDate fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idDistrito")
    private Distrito distrito;

    /**
     * Zonas que componen el local.
     * RF-004: El sistema deberá permitir definir zonas del local (p. ej., VIP, General, Palco).
     * RF-003: La suma de aforos de todas las zonas no debe exceder el aforo total del local.
     */
    @OneToMany(mappedBy = "local", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Zona> zonas = new ArrayList<>();

    /**
     * Calcula la suma de aforos de todas las zonas activas del local.
     * RF-003: Usado para validar que no exceda el aforo total.
     * 
     * @return Suma de aforos de todas las zonas activas
     */
    public Integer getSumaAforosZonas() {
        return zonas.stream()
                .filter(z -> z.getActivo())
                .mapToInt(Zona::getAforoMax)
                .sum();
    }
}
