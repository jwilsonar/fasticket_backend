package pe.edu.pucp.fasticket.repository.fidelizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.ReglaPuntos;
import java.util.List;

@Repository
public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Integer> {
    List<ReglaPuntos> findByActivoTrue();
    List<ReglaPuntos> findByEstadoTrue();
    List<ReglaPuntos> findBySolesPorPuntos(Double solesPorPuntos);
    List<ReglaPuntos> findByTipoRegla(String tipoRegla);
    
}
