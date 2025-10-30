package pe.edu.pucp.fasticket.repository.fidelizacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.Canje;

@Repository
public interface CanjeRepository extends JpaRepository<Canje, Integer> {
    List<Canje> findByOrdenCompra_Cliente_IdPersona(Integer idCliente);
    List<Canje> findByOrdenCompra_IdOrdenCompra(Integer idOrden);
}

