package pe.edu.pucp.fasticket.repository.fidelizacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.DescuentosRealizados;

@Repository
public interface DescuentosRealizadosRepository extends JpaRepository<DescuentosRealizados, Integer> {
    List<DescuentosRealizados> findByOrdenCompra_IdOrdenCompra(Integer idOrden);
}

