package pe.edu.pucp.fasticket.repository.geografia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.geografia.Distrito;

import java.util.List;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Integer> {
    List<Distrito> findByProvincia_IdProvinciaOrderByNombreAsc(Integer idProvincia);
}

