package pe.edu.pucp.fasticket.repository.fidelizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.DescuentosRealizados;
import java.util.List;

@Repository
public interface DescuentosRealizadosRepository extends JpaRepository<DescuentosRealizados, Integer> {
    // Aquí puedes agregar métodos personalizados si los necesitas
    List<DescuentosRealizados> findByActivoTrue();
    List<DescuentosRealizados> findByEstadoTrue();

}
