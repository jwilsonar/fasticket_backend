package pe.edu.pucp.fasticket.repository.geografia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Integer> {
}

