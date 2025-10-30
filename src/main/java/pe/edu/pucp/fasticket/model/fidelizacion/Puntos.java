package pe.edu.pucp.fasticket.model.fidelizacion;

import java.time.LocalDate;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"reglaPuntos", "cliente", "canje"})
@ToString(exclude = {"reglaPuntos", "cliente", "canje"})
@Entity
@Table(name = "puntos")
public class Puntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPuntos")
    private Integer idPuntos;

    @Column(name = "cantPuntos")
    private Integer cantPuntos; // cantidad de puntos al ser ganados

    @Column(name = "fechaVencimiento")
    private LocalDate fechaVencimiento;

    // agregado mikler 30/10 
    @Column(name = "fechaTransaccion")
    private LocalDate fechaTransaccion; 

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoTransaccion")
    private TipoTransaccion tipoTransaccion;

    @Column(name = "activo")
    private Boolean activo = true;

    // --------- relaciones -----------

    @OneToOne(mappedBy = "puntos", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Canje canje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idRegla", nullable = false)
    private ReglaPuntos reglaPuntos;

    // agregado mikler 30/10 relacion con cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;
}
