package pe.edu.pucp.fasticket.model.usuario;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.fidelizacion.ClienteMembresia;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"carroCompras", "ordenesCompra", "membresiaClientes"})
@ToString(callSuper = true, exclude = {"carroCompras", "ordenesCompra", "membresiaClientes"})
@Entity
@Table(name = "Cliente")
@PrimaryKeyJoinColumn(name = "idPersona")
public class Cliente extends Persona {

    @Column(name = "nivel")
    @Enumerated(EnumType.STRING)
    private TipoNivel nivel = TipoNivel.CLASICO;

    @Column(name = "puntosAcumulados")
    private Integer puntosAcumulados = 0;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private CarroCompras carroCompras;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrdenCompra> ordenesCompra = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ClienteMembresia> membresiaClientes = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    public Cliente() {
        super();
        this.setRol(Rol.CLIENTE);
    }
}
