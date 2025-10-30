package pe.edu.pucp.fasticket.repository.fidelizacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.ReglaPuntos;
import pe.edu.pucp.fasticket.model.fidelizacion.TipoRegla;

@Repository
public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Integer> {
    List<ReglaPuntos> findByActivoTrue();
    List<ReglaPuntos> findByTipoReglaAndActivoTrue(TipoRegla tipoRegla);
    List<ReglaPuntos> findByEstadoTrueAndActivoTrue();
}

