package pe.edu.pucp.fasticket.repository.eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.Zona;

@Repository
public interface ZonaRepositorio extends JpaRepository<Zona, Integer> {
    
    List<Zona> findByActivoTrue();
    
    List<Zona> findByLocalIdLocal(Integer idLocal);
    
    List<Zona> findByLocalIdLocalAndActivoTrue(Integer idLocal);
}
