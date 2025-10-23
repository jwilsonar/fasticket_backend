package pe.edu.pucp.fasticket.repository.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.usuario.Persona;

import java.util.Optional;

@Repository
public interface PersonasRepositorio extends JpaRepository<Persona, Integer> {
    
    Optional<Persona> findByEmail(String email);
    
    Optional<Persona> findByDocIdentidad(String docIdentidad);
    
    boolean existsByEmail(String email);
    
    boolean existsByDocIdentidad(String docIdentidad);
}
