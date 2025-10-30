package pe.edu.pucp.fasticket.repository.fidelizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.CodigoPromocional;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoPromocionalRepository extends JpaRepository<CodigoPromocional, Integer> {
    Optional<CodigoPromocional> findByCodigo(String codigo);
    List<CodigoPromocional> findByTipo(String tipo);

    // Ejemplos adicionales:
    List<CodigoPromocional> findByTipoAndActivoTrue(String tipo);
    List<CodigoPromocional> findByFechaExpiracionAfter(java.time.LocalDate fecha);
}
    

