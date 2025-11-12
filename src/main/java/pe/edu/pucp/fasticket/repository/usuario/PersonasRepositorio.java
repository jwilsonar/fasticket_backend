package pe.edu.pucp.fasticket.repository.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.usuario.Persona;
import pe.edu.pucp.fasticket.model.usuario.Rol;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonasRepositorio extends JpaRepository<Persona, Integer> {
    
    Optional<Persona> findByIdPersona(Integer idPersona);

    Optional<Persona> findByEmail(String email);
    
    Optional<Persona> findByDocIdentidad(String docIdentidad);
    
    boolean existsByEmail(String email);
    
    boolean existsByDocIdentidad(String docIdentidad);

    List<Persona> findByActivo(Boolean activo); // agregado 25/10

    List<Persona> findByTipoDocumento(String tipoDocumento); // agregado 25/10

    List<Persona> findByRol(Rol administrador); // agregado 25/10

    List<Persona> findByRolAndActivo(Rol administrador, boolean b);

}
