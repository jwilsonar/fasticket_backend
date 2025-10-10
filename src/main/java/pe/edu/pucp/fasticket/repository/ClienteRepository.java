package pe.edu.pucp.fasticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.usuario.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

}