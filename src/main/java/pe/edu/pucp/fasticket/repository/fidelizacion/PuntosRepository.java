package pe.edu.pucp.fasticket.repository.fidelizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.fasticket.model.fidelizacion.Puntos;
import java.util.List;

@Repository
public interface PuntosRepository extends JpaRepository<Puntos, Integer> {
    // Aquí puedes agregar métodos personalizados si los necesitas
    List<Puntos> findByClienteIdCliente(Integer idCliente);
    List<Puntos> findByEstado(String estado);
    List<Puntos> findByFechaCreacionAfter(java.time.LocalDate fecha);
    List<Puntos> findByFechaCreacionBefore(java.time.LocalDate fecha);
    List<Puntos> findByReglaPuntosIdReglaPuntos(Integer idReglaPuntos);    
}
