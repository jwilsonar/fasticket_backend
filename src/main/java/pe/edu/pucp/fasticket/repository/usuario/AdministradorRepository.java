package pe.edu.pucp.fasticket.repository.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Integer> {
}
