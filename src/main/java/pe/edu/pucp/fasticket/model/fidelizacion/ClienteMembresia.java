package pe.edu.pucp.fasticket.model.fidelizacion;

import java.time.LocalDate;

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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"cliente", "membresia"})
@ToString(exclude = {"cliente", "membresia"})
@Entity
@Table(name = "cliente_membresia")
public class ClienteMembresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente_membresia")
    private Integer idClienteMembresia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoMembresiaCliente estado = EstadoMembresiaCliente.ACTIVA;

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_membresia", nullable = false)
    private Membresia membresia;
}
