package pe.edu.pucp.fasticket.repository.fidelizacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.CodigoPromocional;

@Repository
public interface CodigoPromocionalRepository extends JpaRepository<CodigoPromocional, Integer> {
    Optional<CodigoPromocional> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<CodigoPromocional> findByFechaFinAfter(LocalDateTime fechaActual);
}

