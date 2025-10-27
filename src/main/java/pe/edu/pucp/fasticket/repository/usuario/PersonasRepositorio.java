package pe.edu.pucp.fasticket.repository.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.usuario.Persona;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonasRepositorio extends JpaRepository<Persona, Integer> {
    
    Optional<Persona> findByEmail(String email);
    
    Optional<Persona> findByDocIdentidad(String docIdentidad);
    
    boolean existsByEmail(String email);
    
    boolean existsByDocIdentidad(String docIdentidad);

    Optional<Persona> findById(Integer id); // agregado 25/10

    List<Persona> findByActivo(Boolean activo); // agregado 25/10

}
