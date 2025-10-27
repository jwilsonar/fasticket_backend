package pe.edu.pucp.fasticket.repository.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.eventos.Zona;

import java.util.List;

@Repository
public interface ZonaRepository extends JpaRepository<Zona, Integer> {
    
    List<Zona> findByLocal_IdLocal(Integer idLocal);
    
    List<Zona> findByActivoTrue();
}
