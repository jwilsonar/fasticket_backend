package pe.edu.pucp.fasticket.repository.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.compra.CarroCompras;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

import java.util.Optional;

@Repository
public interface CarroComprasRepository extends JpaRepository<CarroCompras, Integer> {
    
    Optional<CarroCompras> findByCliente(Cliente cliente);
    
    Optional<CarroCompras> findByCliente_IdPersona(Integer idCliente);
    Optional<CarroCompras> findByCliente_IdPersonaAndActivoTrue(Integer idCliente);
}

