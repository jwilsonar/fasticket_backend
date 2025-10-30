package pe.edu.pucp.fasticket.repository.fidelizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.Canje;
import java.util.List;

@Repository
public interface CanjeRepository extends JpaRepository<Canje, Integer> {
    // Aquí puedes agregar métodos personalizados si los necesitas
    List<Canje> findByOrdenCompra_IdOrdenCompra(Integer idOrdenCompra);
    List<Canje> findByPuntos_IdPuntos(Integer idPuntos);
}