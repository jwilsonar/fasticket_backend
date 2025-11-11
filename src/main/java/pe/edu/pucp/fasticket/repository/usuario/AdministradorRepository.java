package pe.edu.pucp.fasticket.repository.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.usuario.Administrador;

import java.util.Optional;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Integer> {
    /**
     * Busca un administrador por su email (que se usa como username en Spring Security)
     * @param email El email del administrador
     * @return Un Optional que contiene al Administrador si se encuentra
     */
    Optional<Administrador> findByEmail(String email);
}
