package pe.edu.pucp.fasticket.model.usuario;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.compra.OrdenCompra;
import pe.edu.pucp.fasticket.model.eventos.Evento;
import pe.edu.pucp.fasticket.model.eventos.Ticket;
import pe.edu.pucp.fasticket.model.fidelizacion.Puntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"carrosCompras", "ordenesCompra"})
@ToString(callSuper = true, exclude = {"carrosCompras", "ordenesCompra"})
@Entity
@Table(name = "Cliente")
@PrimaryKeyJoinColumn(name = "idPersona")
public class Cliente extends Persona {

    @Column(name = "nivel")
    @Enumerated(EnumType.STRING)
    private TipoMembresia nivel = TipoMembresia.BRONCE; 

    @Column(name = "puntosAcumulados")
    private Integer puntosAcumulados = 0;

    // agregado mikler 30/10 relacion con puntos
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Puntos> puntos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CarroCompras> carrosCompras = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrdenCompra> ordenesCompra = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "cliente_favoritos",
            joinColumns = @JoinColumn(name = "id_cliente"),
            inverseJoinColumns = @JoinColumn(name = "id_evento")
    )
    private List<Evento> eventosFavoritos = new ArrayList<>();

    public Cliente() {
        super();
        this.setRol(Rol.CLIENTE);
    }
}
