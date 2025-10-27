package pe.edu.pucp.fasticket.repository.eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.pucp.fasticket.model.eventos.Local;

@Repository
public interface LocalesRepositorio extends JpaRepository<Local, Integer> {
    
    List<Local> findByActivoTrue();
    
    boolean existsByNombreIgnoreCase(String nombre);
    
    List<Local> findByNombreContainingIgnoreCase(String nombre);
}