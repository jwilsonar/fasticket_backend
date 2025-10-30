package pe.edu.pucp.fasticket.repository.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.fidelizacion.TipoMembresia;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    List<Cliente> findByNivel(TipoMembresia nivel);
    Optional<Cliente> findByEmail(String email);
    @Override
    Optional<Cliente> findById(Integer id);
    Boolean existsByEmail(String email);
}

