package pe.edu.pucp.fasticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import java.util.Optional;

@Repository
public interface CarroComprasRepository extends JpaRepository<CarroCompras, Integer> {
    Optional<CarroCompras> findByClienteIdPersona(Integer idCliente);
}