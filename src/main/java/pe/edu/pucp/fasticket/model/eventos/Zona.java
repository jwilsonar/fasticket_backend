package pe.edu.pucp.fasticket.model.eventos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa una zona dentro de un local.
 * RF-004: El sistema deberá permitir definir zonas del local (p. ej., VIP, General, Palco).
 * 
 * @author Equipo Fasticket
 * @version 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"local"})
@ToString(exclude = {"local"})
@Entity
@Table(name = "Zona")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idZona")
    private Integer idZona;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "aforoMax", nullable = false)
    private Integer aforoMax;

    @Column(name = "imagenUrl", length = 500)
    private String imagenUrl;

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

    /**
     * Local al que pertenece esta zona.
     * RF-004: Cada zona debe estar asociada a un local específico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idLocal")
    private Local local;
}
